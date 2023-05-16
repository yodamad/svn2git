import { SpyObject } from './spyobject';
import Spy = jasmine.Spy;

export class MockStateStorageService extends SpyObject {
    getUrlSpy: Spy;
    storeUrlSpy: Spy;

    constructor() {
        super();
        this.setUrlSpy({});
        this.storeUrlSpy = this.spy('storeUrl').andReturn(this);
    }

    setUrlSpy(json) {
        this.getUrlSpy = this.spy('getUrl').andReturn(json);
    }

    setResponse(json: any): void {
        this.setUrlSpy(json);
    }
}
