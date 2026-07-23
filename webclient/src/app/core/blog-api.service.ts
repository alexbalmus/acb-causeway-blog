import { Injectable, inject } from '@angular/core';

import { RoService } from './ro.service';
import { RoObjectRef, RoObjectRepr } from './ro.types';

const BLOGS_SERVICE = '/restful/services/blog.Blogs';
const BLOG_TYPE = 'blog.Blog';
const POST_TYPE = 'blog.Post';

export interface BlogView {
  instanceId: string;
  title: string;
  name: string;
  handle: string;
  /** member id -> reason the member is currently disabled (absent = enabled) */
  disabled: Record<string, string>;
}

export interface PostView {
  instanceId: string;
  title: string;
  name: string;
  content: string | null;
  pictureDescription: string | null;
  /** server-rendered HTML previews (same markup the Wicket UI shows) */
  picturePreviewHtml: string | null;
  contentPreviewHtml: string | null;
  blogRef: { domainType: string; instanceId: string; title: string } | null;
  disabled: Record<string, string>;
}

export interface RoBlobValue {
  name: string;
  mimeType: string;
  bytes: string;
}

/** Reads a browser File into the {name, mimeType, bytes} shape RO expects for images. */
export function fileToRoBlob(file: File): Promise<RoBlobValue> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Could not read the selected file'));
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
      resolve({ name: file.name, mimeType: file.type || 'image/png', bytes: base64 });
    };
    reader.readAsDataURL(file);
  });
}

/**
 * Domain-level facade over the generic Restful Objects client: one method per
 * action/property that the Causeway app exposes for Blogs and Posts.
 */
@Injectable({ providedIn: 'root' })
export class BlogApiService {
  private readonly ro = inject(RoService);

  // --- Blogs service (menu) actions -------------------------------------

  /** listAll: blogs owned by the current user (prototyping-mode action). */
  async myBlogs(): Promise<BlogView[]> {
    const result = await this.ro.invokeSafe(BLOGS_SERVICE, 'listAll');
    return this.loadBlogs(await this.ro.resolveListResult(result));
  }

  async createBlog(name: string, handle: string): Promise<BlogView> {
    const repr = await this.ro.invokeNonIdempotent(BLOGS_SERVICE, 'create', { name, handle });
    return this.toBlogView(repr);
  }

  async changeHandle(handle: string): Promise<void> {
    await this.ro.invokeNonIdempotent(BLOGS_SERVICE, 'changeHandle', { handle });
  }

  async findBlogs(name: string): Promise<BlogView[]> {
    const result = await this.ro.invokeSafe(BLOGS_SERVICE, 'findByNameContaining', { name });
    return this.loadBlogs(await this.ro.resolveListResult(result));
  }

  /** Whether the current user already has a handle (drives the change-handle UI). */
  async serviceDisabledReasons(): Promise<Record<string, string>> {
    const repr = await this.ro.getObjectByHref(BLOGS_SERVICE);
    return this.disabledReasons(repr);
  }

  // --- Blog object ------------------------------------------------------

  async getBlog(instanceId: string): Promise<BlogView> {
    return this.toBlogView(await this.ro.getObject(BLOG_TYPE, instanceId));
  }

  async blogPosts(instanceId: string): Promise<RoObjectRef[]> {
    return this.ro.getCollectionItems(this.ro.objectHref(BLOG_TYPE, instanceId), 'posts');
  }

  async renameBlog(instanceId: string, name: string): Promise<BlogView> {
    const href = this.ro.objectHref(BLOG_TYPE, instanceId);
    return this.toBlogView(await this.ro.invokeIdempotent(href, 'updateName', { name }));
  }

  async createPost(
    blogInstanceId: string,
    title: string,
    content: string | null,
    picture: RoBlobValue | null,
    pictureDescription: string | null,
  ): Promise<PostView> {
    const href = this.ro.objectHref(BLOG_TYPE, blogInstanceId);
    const repr = await this.ro.invokeNonIdempotent(href, 'createPost', {
      title,
      content,
      picture,
      pictureDescription,
    });
    return this.toPostView(repr);
  }

  async deletePostByTitle(blogInstanceId: string, title: string): Promise<void> {
    const href = this.ro.objectHref(BLOG_TYPE, blogInstanceId);
    await this.ro.invokeNonIdempotent(href, 'deletePost', { title });
  }

  async deleteBlog(instanceId: string): Promise<void> {
    await this.ro.invokeNonIdempotent(this.ro.objectHref(BLOG_TYPE, instanceId), 'delete');
  }

  // --- Post object ------------------------------------------------------

  async getPost(instanceId: string): Promise<PostView> {
    return this.toPostView(await this.ro.getObject(POST_TYPE, instanceId));
  }

  async renamePost(instanceId: string, name: string): Promise<PostView> {
    const href = this.ro.objectHref(POST_TYPE, instanceId);
    return this.toPostView(await this.ro.invokeIdempotent(href, 'updateTitle', { name }));
  }

  async updatePostContent(instanceId: string, content: string | null): Promise<void> {
    await this.ro.setProperty(this.ro.objectHref(POST_TYPE, instanceId), 'content', content);
  }

  async updatePictureDescription(instanceId: string, description: string | null): Promise<void> {
    await this.ro.setProperty(
      this.ro.objectHref(POST_TYPE, instanceId),
      'pictureDescription',
      description,
    );
  }

  async updatePicture(
    instanceId: string,
    picture: RoBlobValue | null,
    pictureDescription: string | null,
  ): Promise<PostView> {
    const href = this.ro.objectHref(POST_TYPE, instanceId);
    const repr = await this.ro.invokeIdempotent(href, 'updatePicture', {
      picture,
      pictureDescription,
    });
    return this.toPostView(repr);
  }

  async clearPicture(instanceId: string): Promise<PostView> {
    const href = this.ro.objectHref(POST_TYPE, instanceId);
    return this.toPostView(await this.ro.invokeIdempotent(href, 'clearPicture'));
  }

  async deletePost(instanceId: string): Promise<void> {
    await this.ro.invokeNonIdempotent(this.ro.objectHref(POST_TYPE, instanceId), 'delete');
  }

  // --- mapping ----------------------------------------------------------

  private async loadBlogs(refs: RoObjectRef[]): Promise<BlogView[]> {
    return Promise.all(refs.map(async (ref) => this.toBlogView(await this.ro.getObjectByHref(ref.href))));
  }

  private toBlogView(repr: RoObjectRepr): BlogView {
    return {
      instanceId: repr.instanceId ?? '',
      title: repr.title,
      name: (repr.members['name']?.value as string) ?? '',
      handle: (repr.members['handle']?.value as string) ?? '',
      disabled: this.disabledReasons(repr),
    };
  }

  private toPostView(repr: RoObjectRepr): PostView {
    const blogLink = repr.members['blog']?.value as { href?: string; title?: string } | undefined;
    let blogRef: PostView['blogRef'] = null;
    if (blogLink?.href) {
      const match = /\/objects\/([^/]+)\/([^/]+)/.exec(blogLink.href);
      if (match) {
        blogRef = { domainType: match[1], instanceId: match[2], title: blogLink.title ?? '' };
      }
    }
    return {
      instanceId: repr.instanceId ?? '',
      title: repr.title,
      name: (repr.members['title']?.value as string) ?? repr.title,
      content: (repr.members['content']?.value as string | undefined) ?? null,
      pictureDescription:
        (repr.members['pictureDescription']?.value as string | undefined) ?? null,
      picturePreviewHtml: (repr.members['picturePreview']?.value as string | undefined) ?? null,
      contentPreviewHtml: (repr.members['contentPreview']?.value as string | undefined) ?? null,
      blogRef,
      disabled: this.disabledReasons(repr),
    };
  }

  private disabledReasons(repr: RoObjectRepr): Record<string, string> {
    const reasons: Record<string, string> = {};
    for (const [id, member] of Object.entries(repr.members ?? {})) {
      if (member.disabledReason) {
        reasons[id] = member.disabledReason;
      }
    }
    return reasons;
  }
}
