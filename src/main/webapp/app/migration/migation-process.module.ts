import { Svn2GitSharedModule } from 'app/shared';
import { RouterModule } from '@angular/router';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CHECK_ROUTE, MIGRATION_ROUTE } from 'app/migration/migation-process.route';
import { MigrationStepperComponent } from 'app/migration/migration-stepper.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MigrationCheckComponent } from 'app/migration/migration-check.component';

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild([MIGRATION_ROUTE, CHECK_ROUTE]), ReactiveFormsModule],
    declarations: [MigrationStepperComponent, MigrationCheckComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationModule {}
