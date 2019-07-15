import { IMigration } from 'app/shared/model//migration.model';

export const enum Reason {
    EXTENSION = 'EXTENSION',
    SIZE = 'SIZE'
}

export interface IMigrationRemovedFile {
    id?: number;
    svnLocation?: string;
    path?: string;
    reason?: Reason;
    fileSize?: number;
    migration?: IMigration;
}

export class MigrationRemovedFile implements IMigrationRemovedFile {
    constructor(
        public id?: number,
        public svnLocation?: string,
        public path?: string,
        public reason?: Reason,
        public fileSize?: number,
        public migration?: IMigration
    ) {}
}
