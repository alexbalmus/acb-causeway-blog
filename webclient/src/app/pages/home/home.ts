import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { BlogApiService, BlogView } from '../../core/blog-api.service';
import { MessagesService } from '../../core/messages.service';
import { RoError } from '../../core/ro.types';

type Panel = 'none' | 'newBlog' | 'changeHandle' | 'find';

@Component({
  selector: 'app-home',
  imports: [FormsModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly api = inject(BlogApiService);
  private readonly messages = inject(MessagesService);
  private readonly router = inject(Router);

  readonly blogs = signal<BlogView[]>([]);
  readonly loading = signal(true);
  readonly panel = signal<Panel>('none');
  readonly busy = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly searchResults = signal<BlogView[] | null>(null);
  readonly changeHandleDisabled = signal<string | null>(null);

  readonly heading = computed(() => {
    const count = this.blogs().length;
    return count === 1 ? '1 blog' : `${count} blogs`;
  });

  /** the current user's handle, inferred from their blogs (all share one handle) */
  readonly currentHandle = computed(() => this.blogs()[0]?.handle ?? null);

  newBlogName = '';
  newBlogHandle = '';
  newHandle = '';
  searchText = '';

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.blogs.set(await this.api.myBlogs());
      const disabled = await this.api.serviceDisabledReasons();
      this.changeHandleDisabled.set(disabled['changeHandle'] ?? null);
    } catch (error) {
      this.messages.error(this.describe(error, 'Could not load your blogs'));
    } finally {
      this.loading.set(false);
    }
  }

  openPanel(panel: Panel): void {
    this.fieldErrors.set({});
    this.panel.set(this.panel() === panel ? 'none' : panel);
    if (panel === 'newBlog') {
      this.newBlogName = '';
      this.newBlogHandle = this.currentHandle() ?? '';
    }
    if (panel === 'changeHandle') {
      this.newHandle = this.currentHandle() ?? '';
    }
    if (panel === 'find') {
      this.searchText = '';
      this.searchResults.set(null);
    }
  }

  async createBlog(): Promise<void> {
    await this.run(async () => {
      const blog = await this.api.createBlog(this.newBlogName.trim(), this.newBlogHandle.trim());
      this.messages.info(`Created '${blog.title}'`);
      this.panel.set('none');
      // jump straight to the new blog so the user can start adding posts
      await this.router.navigate(['/blogs', blog.instanceId]);
    });
  }

  async changeHandle(): Promise<void> {
    const handle = this.newHandle.trim();
    if (!confirm(`Change your handle to '@${handle}'? All your blogs will be updated.`)) {
      return;
    }
    await this.run(async () => {
      await this.api.changeHandle(handle);
      this.messages.info(`Handle changed to '@${handle}'`);
      this.panel.set('none');
      await this.refresh();
    });
  }

  async deleteBlog(blog: BlogView): Promise<void> {
    if (!confirm(`Delete '${blog.title}' and all its posts?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteBlog(blog.instanceId);
      this.messages.info(`'${blog.title}' and its posts have been deleted`);
      await this.refresh();
    });
  }

  async search(): Promise<void> {
    await this.run(async () => {
      this.searchResults.set(await this.api.findBlogs(this.searchText.trim()));
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
