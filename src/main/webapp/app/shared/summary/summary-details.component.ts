import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { MigrationHistory } from 'app/shared/model/migration-history.model';
import { MigrationService } from 'app/entities/migration';

@Component({
    selector: 'jhi-details-card',
    templateUrl: 'summary-details.component.html',
    styleUrls: ['summary-card.component.css']
})
export class DetailsCardComponent implements OnInit, OnDestroy {
    // Migration history refresh
    static migrationHistoryRefreshInterval = 2000; // ms
    migrationHistoryRefreshTimer;

    @Input() migration: IMigration;
    @Output() migrationUpdated = new EventEmitter<void>();

    displayedColumns: string[] = ['date', 'step', 'data'];
    histories: MigrationHistory[] = [];

    isLoading = false;

    constructor(private _migrationService: MigrationService) {}

    /**
     * On init
     */
    ngOnInit(): void {
        if (this.migration !== undefined && this.migration.id !== undefined) {
            this.loadHistory();
            if ([StatusEnum.WAITING, StatusEnum.RUNNING].includes(this.migration.status)) {
                this.migrationHistoryRefreshTimer = setInterval(
                    () => this.loadHistory(),
                    DetailsCardComponent.migrationHistoryRefreshInterval
                );
            }
        }
    }

    /**
     * On destroy
     */
    ngOnDestroy(): void {
        this.stopHistoryRefresh();
    }

    /**
     * Load history of migration
     */
    loadHistory(): void {
        if (this.isLoading) {
            // If slow database
            return;
        }
        this.isLoading = true;
        this._migrationService.findHistories(this.migration.id).subscribe(res => {
            if (res.body) {
                let newHistories = res.body;
                newHistories = newHistories.reverse();
                if (this.histories !== newHistories) {
                    this.histories = newHistories;
                    this.migrationUpdated.emit();
                }
                this.isLoading = false;
            }
        });
    }

    /**
     * Stop history refresh
     */
    stopHistoryRefresh(): void {
        clearInterval(this.migrationHistoryRefreshTimer);
        this.migrationHistoryRefreshTimer = null;
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
            return 'badge-danger';
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
