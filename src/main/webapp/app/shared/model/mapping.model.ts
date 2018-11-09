import { IMigration } from 'app/shared/model/migration.model';

export interface IMapping {
    id?: number;
    svnDirectory?: string;
    regex?: string;
    gitDirectory?: string;
    migration?: IMigration;
}

export class Mapping implements IMapping {
    constructor(
        public id?: number,
        public svnDirectory?: string,
        public regex?: string,
        public gitDirectory?: string,
        public migration?: IMigration
    ) {}
}
