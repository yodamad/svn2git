import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MigrationService } from 'app/entities/migration';
import { IMapping } from 'app/shared/model/mapping.model';

/**
 * Migration summary component
 */
@Component({
    selector: 'jhi-summary-mappings',
    templateUrl: 'summary-mappings.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryMappingsComponent implements OnInit {
    @Input() isReadOnly = false;
    @Input() migrationId: number;
    @Input() mappings: IMapping[];
    @Input() overrideStaticMappings = false;
    @Output() deleteMapping = new EventEmitter<IMapping>();
    @Output() toggleMapping = new EventEmitter<IMapping>();

    displayedColumns: string[] = ['svn', 'regex', 'git', 'toggleMapping'];

    /**
     * Constructor
     * @param _migrationService the migration service
     */
    constructor(private _migrationService: MigrationService) {}

    /**
     * On init
     */
    ngOnInit() {
        if (!this.isReadOnly) {
            this.displayedColumns = ['delete', ...this.displayedColumns];
        }
        if (this.migrationId) {
            this._migrationService.findMappings(this.migrationId).subscribe(res => {
                this.mappings = res.body;
                this.sort();
            });
        }
        if (this.mappings) {
            this.sort();
        }
    }

    /**
     * Apply sort
     */
    sort(): void {
        this.mappings.sort((mapping1: IMapping, mapping2: IMapping) => {
            if (mapping1.svnDirectory > mapping2.svnDirectory) {
                return 1;
            }
            if (mapping1.svnDirectory < mapping2.svnDirectory) {
                return -1;
            }
            return 0;
        });
    }

    /**
     * Delete mapping
     * @param mapping the mapping
     */
    delete(mapping: IMapping): void {
        this.deleteMapping.emit(mapping);
    }

    /**
     * Toggle mapping
     * @param event the event
     * @param mapping the mapping
     */
    toggle(event, mapping: IMapping): void {
        mapping.svnDirectoryDelete = !mapping.svnDirectoryDelete;
        event['mapping'] = mapping;
        this.toggleMapping.emit(event);
    }
}
