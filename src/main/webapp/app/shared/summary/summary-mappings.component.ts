import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration';
import { Mapping } from 'app/shared/model/mapping.model';

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
    mappings: Mapping[] = [];

    displayedColumns: string[] = ['svn', 'icon', 'git'];

    constructor(private _migrationService: MigrationService) {}

    ngOnInit() {
        if (this.migration !== undefined && this.migration.id !== undefined) {
            console.log('Loading mappings for migration ' + this.migration.id);
            this._migrationService.findMappings(this.migration.id).subscribe(res => (this.mappings = res.body));
        }
    }
}
