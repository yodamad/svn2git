export interface IStaticExtension {
    id?: number;
    value?: string;
    description?: string;
    enabled?: boolean;
    name?: string;
}

export class StaticExtension implements IStaticExtension {
    constructor(public id?: number, public value?: string, public description?: string, public enabled?: boolean, public name?: string) {
        this.enabled = this.enabled || false;
    }
}

export class Extension extends StaticExtension {
    constructor(
        public id?: number,
        public value?: string,
        public description?: string,
        public enabled?: boolean,
        public name?: string,
        public isStatic = true
    ) {
        super(id, value, description, enabled, name);
    }
}
