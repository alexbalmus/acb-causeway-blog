import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { BlogApiService, BlogView, fileToRoBlob, RoBlobValue } from '../../core/blog-api.service';
import { MessagesService } from '../../core/messages.service';
import { RoError, RoObjectRef } from '../../core/ro.types';

type Panel = 'none' | 'rename' | 'newPost';

@Component({
  selector: 'app-blog-detail',
  imports: [FormsModule, RouterLink],
  templateUrl: './blog-detail.html',
  styleUrl: './blog-detail.css',
})
export class BlogDetail implements OnInit {
  private readonly api = inject(BlogApiService);
  private readonly messages = inject(MessagesService);
  private readonly router = inject(Router);

  /** route param (withComponentInputBinding) */
  readonly id = input.required<string>();

  readonly blog = signal<BlogView | null>(null);
  readonly posts = signal<RoObjectRef[]>([]);
  readonly loading = signal(true);
  readonly panel = signal<Panel>('none');
  readonly busy = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});

  renameValue = '';
  postTitle = '';
  postContent = '';
  postPictureDescription = '';
  postPictureFile: File | null = null;

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const [blog, posts] = await Promise.all([
        this.api.getBlog(this.id()),
        this.api.blogPosts(this.id()),
      ]);
      this.blog.set(blog);
      this.posts.set(posts);
    } catch (error) {
      this.messages.error(this.describe(error, 'Could not load the blog'));
    } finally {
      this.loading.set(false);
    }
  }

  disabledReason(memberId: string): string | null {
    return this.blog()?.disabled[memberId] ?? null;
  }

  openPanel(panel: Panel): void {
    this.fieldErrors.set({});
    this.panel.set(this.panel() === panel ? 'none' : panel);
    if (panel === 'rename') {
      this.renameValue = this.blog()?.name ?? '';
    }
    if (panel === 'newPost') {
      this.postTitle = '';
      this.postContent = '';
      this.postPictureDescription = '';
      this.postPictureFile = null;
    }
  }

  onPictureSelected(event: Event): void {
    const files = (event.target as HTMLInputElement).files;
    this.postPictureFile = files && files.length > 0 ? files[0] : null;
  }

  async rename(): Promise<void> {
    await this.run(async () => {
      const blog = await this.api.renameBlog(this.id(), this.renameValue.trim());
      this.blog.set(blog);
      this.panel.set('none');
      this.messages.info('Blog renamed');
    });
  }

  async createPost(): Promise<void> {
    await this.run(async () => {
      let picture: RoBlobValue | null = null;
      if (this.postPictureFile) {
        picture = await fileToRoBlob(this.postPictureFile);
      }
      const post = await this.api.createPost(
        this.id(),
        this.postTitle.trim(),
        this.postContent.trim() || null,
        picture,
        this.postPictureDescription.trim() || null,
      );
      this.messages.info(`Created '${post.title}'`);
      this.panel.set('none');
      // jump straight to the new post
      await this.router.navigate(['/posts', post.instanceId]);
    });
  }

  async deletePost(post: RoObjectRef): Promise<void> {
    if (!confirm(`Delete post '${post.title}'?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deletePostByTitle(this.id(), post.title);
      this.messages.info(`'${post.title}' deleted`);
      await this.refresh();
    });
  }

  async deleteBlog(): Promise<void> {
    const blog = this.blog();
    if (!blog || !confirm(`Delete '${blog.title}' and all its posts?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteBlog(this.id());
      this.messages.info(`'${blog.title}' and its posts have been deleted`);
      await this.router.navigate(['/']);
    });
  }

  private async run(work: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.fieldErrors.set({});
    try {
      await work();
    } catch (error) {
      if (error instanceof RoError && error.validation) {
        this.fieldErrors.set(error.validation.params);
        if (Object.keys(error.validation.params).length === 0) {
          this.messages.error(error.validation.general ?? error.message);
        }
      } else {
        this.messages.error(this.describe(error, 'The action failed'));
      }
    } finally {
      this.busy.set(false);
    }
  }

  private describe(error: unknown, fallback: string): string {
    return error instanceof RoError ? error.message : fallback;
  }
}
