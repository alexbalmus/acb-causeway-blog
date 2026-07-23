/**
 * Minimal typings for the Restful Objects representations produced by
 * Apache Causeway's REST API (restfulobjects viewer).
 */

export interface RoLink {
  rel: string;
  href: string;
  method: string;
  type?: string;
  title?: string;
}

export interface RoMember {
  id: string;
  memberType: 'property' | 'collection' | 'action';
  links: RoLink[];
  value?: unknown;
  disabledReason?: string;
  extensions?: Record<string, unknown>;
}

export interface RoObjectRepr {
  links: RoLink[];
  extensions: { oid?: string; isService?: boolean; isPersistent?: boolean };
  title: string;
  domainType?: string;
  instanceId?: string;
  serviceId?: string;
  members: Record<string, RoMember>;
}

export interface RoCollectionRepr {
  id: string;
  value: RoLink[];
  disabledReason?: string;
}

/** One entry of a collection or action list result: a link to a domain object. */
export interface RoObjectRef {
  href: string;
  title: string;
  domainType: string;
  instanceId: string;
}

/** Per-parameter validation failure reported by a 422 response. */
export interface RoValidationErrors {
  /** parameter/property id -> invalid reason */
  params: Record<string, string>;
  /** overall reason (x-ro-invalidReason) */
  general?: string;
}

/** Error raised by RoService with the useful parts extracted. */
export class RoError extends Error {
  constructor(
    override readonly message: string,
    readonly status: number,
    readonly validation?: RoValidationErrors,
  ) {
    super(message);
  }
}

export const DOMAIN_OBJECT_LIST_TYPE = 'causeway.applib.DomainObjectList';

/** Extracts {domainType, instanceId} from an .../objects/{type}/{id}[...] href. */
export function parseObjectHref(href: string): { domainType: string; instanceId: string } {
  const match = /\/objects\/([^/]+)\/([^/]+)/.exec(href);
  if (!match) {
    throw new Error(`Not an object href: ${href}`);
  }
  return { domainType: match[1], instanceId: match[2] };
}

export function toObjectRef(link: RoLink): RoObjectRef {
  const { domainType, instanceId } = parseObjectHref(link.href);
  return { href: link.href, title: link.title ?? '', domainType, instanceId };
}
