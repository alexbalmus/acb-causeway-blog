import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RoService } from './ro.service';
import { DOMAIN_OBJECT_LIST_TYPE, RoError, RoObjectRepr } from './ro.types';

describe('Restful Objects HTTP contract', () => {
  let ro: RoService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    ro = TestBed.inject(RoService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('encodes safe action arguments, including punctuation and Unicode', async () => {
    const result = ro.invokeSafe('/restful/services/blog.Blogs', 'findByNameContaining', { name: 'café & Java?' });
    const req = http.expectOne(r => r.url.endsWith('/findByNameContaining/invoke'));
    expect(req.request.method).toBe('GET');
    expect(JSON.parse(req.request.params.get('x-causeway-querystring')!)).toEqual({ name: { value: 'café & Java?' } });
    req.flush({}); await result;
    const empty = ro.invokeSafe('/restful/services/blog.Blogs', 'listAll');
    http.expectOne(r => r.params.get('x-causeway-querystring') === '{}').flush({});
    await empty;
  });

  for (const method of ['POST', 'PUT']) {
    it(`wraps ${method} action parameters and retains nulls`, async () => {
      const result = method === 'POST'
        ? ro.invokeNonIdempotent('/restful/objects/blog.Blog/1', 'createPost', { title: 'Post', content: null })
        : ro.invokeIdempotent('/restful/objects/blog.Blog/1', 'createPost', { title: 'Post', content: null });
      const req = http.expectOne('/restful/objects/blog.Blog/1/actions/createPost/invoke');
      expect(req.request.method).toBe(method);
      expect(req.request.body).toEqual({ title: { value: 'Post' }, content: { value: null } });
      req.flush({}); await result;
    });
  }

  it('writes a property using the property representation', async () => {
    const result = ro.setProperty('/restful/objects/blog.Post/2', 'content', null);
    const req = http.expectOne('/restful/objects/blog.Post/2/properties/content');
    expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual({ value: null });
    req.flush(null); await result;
  });

  it('follows absolute API collection links through the same-origin proxy', async () => {
    const result = ro.resolveListResult({ domainType: DOMAIN_OBJECT_LIST_TYPE,
      links: [{ rel: 'self', href: 'http://backend:8080/restful/objects/list/1', method: 'GET' }] } as RoObjectRepr);
    http.expectOne('/restful/objects/list/1/collections/objects').flush({ value: [
      { href: 'http://backend:8080/restful/objects/blog.Blog/42', title: 'Journal', method: 'GET' }] });
    expect(await result).toEqual([{ href: 'http://backend:8080/restful/objects/blog.Blog/42', title: 'Journal', domainType: 'blog.Blog', instanceId: '42' }]);
  });

  for (const body of [{ title: { invalidReason: 'Duplicate title' } }, { invalidReason: 'Duplicate title' }, { 'x-ro-invalidReason': 'Duplicate title' }]) {
    it('preserves validation feedback from ' + JSON.stringify(body), async () => {
      const result = ro.setProperty('/restful/objects/blog.Post/2', 'title', 'Duplicate');
      const rejected = expectAsync(result).toBeRejectedWith(jasmine.objectContaining({ message: 'Duplicate title', status: 422 }));
      http.expectOne('/restful/objects/blog.Post/2/properties/title').flush(body, { status: 422, statusText: 'Unprocessable Entity' });
      await rejected;
    });
  }

  it('reports server errors without losing the HTTP status', async () => {
    const result = ro.getObject('blog.Post', 'missing');
    const rejected = expectAsync(result).toBeRejectedWith(new RoError('Missing post', 404));
    http.expectOne('/restful/objects/blog.Post/missing').flush({ message: 'Missing post' }, { status: 404, statusText: 'Not Found' });
    await rejected;
  });
});
