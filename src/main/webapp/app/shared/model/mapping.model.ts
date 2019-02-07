export interface IMapping {
    id?: number;
    svnDirectory?: string;
    regex?: string;
    gitDirectory?: string;
    migration?: number;
    isStatic?: boolean;
}

export class Mapping implements IMapping {
    constructor(
        public id?: number,
        public svnDirectory?: string,
        public regex?: string,
        public gitDirectory?: string,
        public migration?: number,
        public isStatic?: boolean
    ) {}
}
