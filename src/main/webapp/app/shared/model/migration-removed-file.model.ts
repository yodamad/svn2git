import { IMigration } from 'app/shared/model/migration.model';

export const enum Reason {
    EXTENSION = 'EXTENSION',
    SIZE = 'SIZE'
}

export interface IMigrationRemovedFile {
    id?: number;
    path?: string;
    reason?: Reason;
    migration?: IMigration;
}

export class MigrationRemovedFile implements IMigrationRemovedFile {
    constructor(public id?: number, public path?: string, public reason?: Reason, public migration?: IMigration) {}
}
