import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  DOMAIN_OBJECT_LIST_TYPE,
  RoCollectionRepr,
  RoError,
  RoLink,
  RoObjectRef,
  RoObjectRepr,
  RoValidationErrors,
  toObjectRef,
} from './ro.types';

/**
 * Generic client for the Restful Objects API exposed by Apache Causeway
 * under /restful. Encapsulates the invocation conventions:
 *  - safe actions      -> GET  .../invoke with JSON in x-causeway-querystring
 *  - idempotent        -> PUT  .../invoke with {"param":{"value":...}} body
 *  - non-idempotent    -> POST .../invoke with the same body shape
 *  - editable property -> PUT  .../properties/{id} with {"value":...}
 */
@Injectable({ providedIn: 'root' })
export class RoService {
  private readonly http = inject(HttpClient);

  objectHref(domainType: string, instanceId: string): string {
    return `/restful/objects/${domainType}/${instanceId}`;
  }

  async getObject(domainType: string, instanceId: string): Promise<RoObjectRepr> {
    return this.get<RoObjectRepr>(this.objectHref(domainType, instanceId));
  }

  async getObjectByHref(href: string): Promise<RoObjectRepr> {
    return this.get<RoObjectRepr>(href);
  }

  /** Resolves a collection member to the object references it contains. */
  async getCollectionItems(objectHref: string, collectionId: string): Promise<RoObjectRef[]> {
    const repr = await this.get<RoCollectionRepr>(`${objectHref}/collections/${collectionId}`);
    return (repr.value ?? []).map(toObjectRef);
  }

  /**
   * Fetches an action prompt and returns the default value per parameter
   * (used to mirror the defaultNXxx() supporting methods in the UI).
   */
  async getActionDefaults(
    memberHref: string,
    actionId: string,
  ): Promise<Record<string, unknown>> {
    try {
      const prompt = await this.get<{
        parameters?: Record<string, { default?: unknown }>;
      }>(`${memberHref}/actions/${actionId}`);
      const defaults: Record<string, unknown> = {};
      for (const [name, param] of Object.entries(prompt.parameters ?? {})) {
        if (param && param.default !== undefined) {
          defaults[name] = param.default;
        }
      }
      return defaults;
    } catch {
      // prompts are a nicety; never fail the page for them
      return {};
    }
  }

  /** Invokes a safe (query-only) action; args are plain values. */
  async invokeSafe(
    memberHref: string,
    actionId: string,
    args: Record<string, string> = {},
  ): Promise<RoObjectRepr> {
    // Causeway 4 requires this parameter even for actions without arguments.
    // Arguments use the same {param: {value: ...}} representation as POST/PUT.
    const params = new HttpParams().set(
      'x-causeway-querystring',
      JSON.stringify(this.wrapArgs(args)),
    );
    return this.request<RoObjectRepr>('GET', `${memberHref}/actions/${actionId}/invoke`, {
      params,
    });
  }

  /** Invokes an idempotent action (PUT). Args are already RO-formatted values. */
  async invokeIdempotent(
    memberHref: string,
    actionId: string,
    args: Record<string, unknown> = {},
  ): Promise<RoObjectRepr> {
    return this.request<RoObjectRepr>('PUT', `${memberHref}/actions/${actionId}/invoke`, {
      body: this.wrapArgs(args),
    });
  }

  /** Invokes a non-idempotent action (POST). Args are already RO-formatted values. */
  async invokeNonIdempotent(
    memberHref: string,
    actionId: string,
    args: Record<string, unknown> = {},
  ): Promise<RoObjectRepr> {
    return this.request<RoObjectRepr>('POST', `${memberHref}/actions/${actionId}/invoke`, {
      body: this.wrapArgs(args),
    });
  }

  /** Writes an editable property. */
  async setProperty(objectHref: string, propertyId: string, value: unknown): Promise<void> {
    await this.request<unknown>('PUT', `${objectHref}/properties/${propertyId}`, {
      body: { value },
    });
  }

  /**
   * List-returning actions come back as a causeway.applib.DomainObjectList
   * view model; the actual elements sit behind its "objects" collection.
   */
  async resolveListResult(result: RoObjectRepr): Promise<RoObjectRef[]> {
    if (result?.domainType !== DOMAIN_OBJECT_LIST_TYPE) {
      throw new Error(`Expected a ${DOMAIN_OBJECT_LIST_TYPE} result`);
    }
    const selfLink = result.links.find((l: RoLink) => l.rel === 'self');
    if (!selfLink) {
      return [];
    }
    return this.getCollectionItems(selfLink.href, 'objects');
  }

  private wrapArgs(args: Record<string, unknown>): Record<string, { value: unknown }> {
    const wrapped: Record<string, { value: unknown }> = {};
    for (const [key, value] of Object.entries(args)) {
      wrapped[key] = { value: value ?? null };
    }
    return wrapped;
  }

  private async get<T>(url: string): Promise<T> {
    return this.request<T>('GET', url, {});
  }

  /**
   * The API emits absolute hrefs (http://host:8080/restful/...). Requesting
   * them directly would bypass the same-origin proxy — no Authorization
   * header, blocked by CORS — so every URL is reduced to its /restful path.
   */
  private toApiPath(url: string): string {
    const index = url.indexOf('/restful/');
    return index > 0 ? url.substring(index) : url;
  }

  private async request<T>(
    method: string,
    url: string,
    options: { body?: unknown; params?: HttpParams },
  ): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.request<T>(method, this.toApiPath(url), {
          body: options.body,
          params: options.params,
        }),
      );
    } catch (error) {
      throw this.toRoError(error);
    }
  }

  private toRoError(error: unknown): RoError {
    if (!(error instanceof HttpErrorResponse)) {
      return new RoError('Unexpected error', 0);
    }

    const body = error.error;
    if (error.status === 422 && body && typeof body === 'object') {
      const validation: RoValidationErrors = { params: {} };
      for (const [key, value] of Object.entries(body as Record<string, unknown>)) {
        if (key === 'x-ro-invalidReason') {
          validation.general = String(value);
        } else if (key === 'invalidReason') {
          // property PUT failures report a single top-level reason
          validation.params['value'] = String(value);
        } else if (value && typeof value === 'object' && 'invalidReason' in value) {
          validation.params[key] = String((value as { invalidReason: unknown }).invalidReason);
        }
      }
      const message =
        Object.values(validation.params)[0] ?? validation.general ?? 'Validation failed';
      return new RoError(message, error.status, validation);
    }

    if (typeof body === 'string' && body.length > 0 && body.length < 500) {
      return new RoError(body, error.status);
    }
    if (body && typeof body === 'object' && 'message' in body) {
      return new RoError(String((body as { message: unknown }).message), error.status);
    }
    return new RoError(`Request failed (HTTP ${error.status})`, error.status);
  }
}
