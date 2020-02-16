import { Component, Input, OnInit } from '@angular/core';
import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { MigrationHistory } from 'app/shared/model/migration-history.model';
import { MigrationService } from 'app/entities/migration';

@Component({
    selector: 'jhi-details-card',
    templateUrl: 'summary-details.component.html',
    styleUrls: ['summary-card.component.css']
})
export class DetailsCardComponent implements OnInit {
    @Input() migration: IMigration;

    displayedColumns: string[] = ['step', 'status', 'date', 'data', 'executionTime'];
    histories: MigrationHistory[] = [];

    constructor(private _migrationService: MigrationService) {}

    ngOnInit() {
        if (this.migration !== undefined && this.migration.id !== undefined) {
            console.log('Loading histories for migration ' + this.migration.id);
            this._migrationService.findHistories(this.migration.id).subscribe(res => (this.histories = res.body));
        }
    }

    /**
     * Add class according to status
     * @param status
     */
    cssClass(status: StatusEnum) {
        if (status === StatusEnum.DONE) {
            return 'cell-ok';
        }
        if (status === StatusEnum.FAILED) {
            return 'cell-ko';
        }
        if (status === StatusEnum.RUNNING) {
            return 'badge-primary';
        }
        if (status === StatusEnum.IGNORED) {
            return 'cell-ignored';
        }
        if (status === StatusEnum.DONE_WITH_WARNINGS) {
            return 'cell-warning';
        }
        if (status === StatusEnum.WAITING) {
            return 'cell-waiting';
        }
    }
}
