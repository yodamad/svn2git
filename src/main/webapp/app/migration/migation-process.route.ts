import { Route } from '@angular/router';
import { MigrationStepperComponent } from 'app/migration/migration-stepper.component';
import { MigrationCheckComponent } from 'app/migration/migration-check.component';

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
