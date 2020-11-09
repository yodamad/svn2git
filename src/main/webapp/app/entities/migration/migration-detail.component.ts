import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration/migration.service';
import { DetailsCardComponent } from 'app/shared/summary/summary-details.component';

@Component({
    selector: 'jhi-migration-detail',
    templateUrl: './migration-detail.component.html',
    styleUrls: ['./migration-detail.component.css']
})
export class MigrationDetailComponent implements OnInit {
    @Input() migration: IMigration;
    @Output() startMigration = new EventEmitter<void>();
    @ViewChild('timeline') timeline: DetailsCardComponent;

    migrationStarted = false;

    constructor(private activatedRoute: ActivatedRoute, private _migrationService: MigrationService) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migration }) => {
            if (migration !== undefined) {
                this.migration = migration;
            }
        });
    }

    getMigrationFromUrl(): string {
        return `${this.migration.svnUrl}${this.migration.svnUrl.endsWith('/') ? '' : '/'}${this.migration.svnGroup}`;
    }

    getMigrationToUrl(): string {
        return `${this.migration.gitlabUrl}${this.migration.gitlabUrl.endsWith('/') ? '' : '/'}${this.migration.gitlabGroup}`;
    }

    getStatusIcon(): string {
        return StatusIcons[this.migration.status];
    }

    isStatusIconSpin(): boolean {
        return this.migration.status === StatusEnum.RUNNING;
    }

    getValueToDisplay(value: string, breakLine = false): string {
        const list = value.split(',');
        if (list.length < 2 || !breakLine) {
            return value;
        }
        return '- ' + list.join('<br />- ');
    }

    start(): void {
        this.migrationStarted = true;
        this.startMigration.emit();
    }

    migrationUpdated(): void {
        this._migrationService.find(this.migration.id).subscribe(res => {
            this.migration = res.body;
            if (![StatusEnum.WAITING, StatusEnum.RUNNING].includes(this.migration.status)) {
                this.timeline.stopHistoryRefresh();
            }
        });
    }
}

export const StatusIcons: Record<StatusEnum, string> = {
    [StatusEnum.WAITING]: 'pause',
    [StatusEnum.RUNNING]: 'sync',
    [StatusEnum.DONE]: 'check-circle',
    [StatusEnum.FAILED]: 'times',
    [StatusEnum.IGNORED]: 'forward',
    [StatusEnum.DONE_WITH_WARNINGS]: 'exclamation-circle'
};
