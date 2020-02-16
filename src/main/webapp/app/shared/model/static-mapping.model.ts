export interface IStaticMapping {
    id?: number;
    svnDirectory?: string;
    regex?: string;
    gitDirectory?: string;
    svnDirectoryDelete?: boolean;
}

export class StaticMapping implements IStaticMapping {
    constructor(
        public id?: number,
        public svnDirectory?: string,
        public regex?: string,
        public gitDirectory?: string,
        public svnDirectoryDelete?: boolean
    ) {
        this.svnDirectoryDelete = this.svnDirectoryDelete || false;
    }
}
