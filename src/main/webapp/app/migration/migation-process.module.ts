import { Svn2GitSharedModule } from 'app/shared';
import { RouterModule } from '@angular/router';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { MIGRATION_ROUTE } from 'app/migration/migation-process.route';
import { MigrationStepperComponent } from 'app/migration/migration-stepper.component';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
    imports: [Svn2GitSharedModule, RouterModule.forChild([MIGRATION_ROUTE]), ReactiveFormsModule],
    declarations: [MigrationStepperComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class Svn2GitMigrationModule {}
