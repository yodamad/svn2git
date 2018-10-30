import { Moment } from 'moment';
import { IMigration } from 'app/shared/model//migration.model';

export const enum StepEnum {
    GITLAB_PROJECT_CREATION = 'GITLAB_PROJECT_CREATION',
    SVN_CHECKOUT = 'SVN_CHECKOUT',
    GIT_CLEANING = 'GIT_CLEANING',
    GIT_PUSH = 'GIT_PUSH',
    GIT_CLONE = 'GIT_CLONE'
}

export const enum StatusEnum {
    WAITING = 'WAITING',
    RUNNING = 'RUNNING',
    DONE = 'DONE',
    FAILED = 'FAILED'
}

export interface IMigrationHistory {
    id?: number;
    step?: StepEnum;
    status?: StatusEnum;
    date?: Moment;
    data?: string;
    migration?: IMigration;
}

export class MigrationHistory implements IMigrationHistory {
    constructor(
        public id?: number,
        public step?: StepEnum,
        public status?: StatusEnum,
        public date?: Moment,
        public data?: string,
        public migration?: IMigration
    ) {}
}
