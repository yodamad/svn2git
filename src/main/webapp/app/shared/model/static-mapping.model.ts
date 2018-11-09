export interface IStaticMapping {
    id?: number;
    svnDirectory?: string;
    regex?: string;
    gitDirectory?: string;
}

export class StaticMapping implements IStaticMapping {
    constructor(public id?: number, public svnDirectory?: string, public regex?: string, public gitDirectory?: string) {}
}
