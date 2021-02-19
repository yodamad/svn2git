import { Route } from '@angular/router';
import { MigrationStepperComponent } from 'app/migration/migration-stepper.component';
import { MigrationCheckComponent } from 'app/migration/migration-check.component';
import { MigrationInitComponent } from 'app/migration/migration-init.component';

export const MIGRATION_ROUTE: Route = {
    path: 'migration-process',
    component: MigrationStepperComponent,
    data: {
        authorities: [],
        pageTitle: 'migration.title'
    }
};

export const CHECK_ROUTE: Route = {
    path: 'migration-check',
    component: MigrationCheckComponent,
    data: {
        authorities: [],
        pageTitle: 'migration.title'
    }
};

export const MIGRATION_INIT: Route = {
    path: 'migration-init',
    component: MigrationInitComponent,
    data: {
        authorities: [],
        pageTitle: 'svn2GitApp.migration.init.title'
    }
};
