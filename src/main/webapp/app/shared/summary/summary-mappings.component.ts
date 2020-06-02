import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration';

/**
 * Migration summary component
 */
@Component({
    selector: 'jhi-summary-mappings',
    templateUrl: 'summary-mappings.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryMappingsComponent implements OnInit {
    @Input() migration: IMigration;

    displayedColumns: string[] = ['svn', 'icon', 'git', 'svnDirectoryDelete'];

    constructor(private _migrationService: MigrationService) {}

    ngOnInit() {
        if (
            this.migration !== undefined &&
            this.migration.id !== undefined &&
            (this.migration.mappings === undefined || this.migration.mappings === null || this.migration.mappings.length === 0)
        ) {
            console.log('Loading mappings for migration ' + this.migration.id);
            this._migrationService.findMappings(this.migration.id).subscribe(res => {
                this.migration.mappings = res.body;
            });
        }
    }
}
