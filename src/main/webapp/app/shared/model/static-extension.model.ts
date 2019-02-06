export interface IStaticExtension {
    id?: number;
    value?: string;
    description?: string;
    enabled?: boolean;
}

export class StaticExtension implements IStaticExtension {
    constructor(public id?: number, public value?: string, public description?: string, public enabled?: boolean) {
        this.enabled = this.enabled || false;
    }
}
