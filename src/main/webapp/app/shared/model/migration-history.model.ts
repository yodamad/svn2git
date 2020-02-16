import { Moment } from 'moment';
import { IMigration } from 'app/shared/model//migration.model';

export const enum StepEnum {
    INIT = 'INIT',
    GITLAB_PROJECT_CREATION = 'GITLAB_PROJECT_CREATION',
    SVN_CHECKOUT = 'SVN_CHECKOUT',
    GIT_CLEANING = 'GIT_CLEANING',
    GIT_PUSH = 'GIT_PUSH',
    GIT_CLONE = 'GIT_CLONE',
    CLEANING = 'CLEANING',
    BRANCH_CLEAN = 'BRANCH_CLEAN',
    TAG_CLEAN = 'TAG_CLEAN',
    README_MD = 'README_MD'
}

export const enum StatusEnum {
    WAITING = 'WAITING',
    RUNNING = 'RUNNING',
    DONE = 'DONE',
    FAILED = 'FAILED',
    DONE_WITH_WARNINGS = 'DONE_WITH_WARNINGS'
}

export interface IMigrationHistory {
    id?: number;
    step?: StepEnum;
    status?: StatusEnum;
    date?: Moment;
    data?: string;
    migration?: IMigration;
    executionTime?: number;
}

export class MigrationHistory implements IMigrationHistory {
    constructor(
        public id?: number,
        public step?: StepEnum,
        public status?: StatusEnum,
        public date?: Moment,
        public data?: string,
        public migration?: IMigration,
        public executionTime?: number
    ) {}
}
