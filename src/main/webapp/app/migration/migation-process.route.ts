import { Route } from '@angular/router';
import { MigrationStepperComponent } from 'app/migration/migration-stepper.component';

export const MIGRATION_ROUTE: Route = {
    path: 'migration-process',
    component: MigrationStepperComponent,
    data: {
        authorities: [],
        pageTitle: 'migration.title'
    }
};
