import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration/migration.service';
import { DetailsCardComponent } from 'app/shared/summary/summary-details.component';
import { MigrationProcessService } from 'app/migration/migration-process.service';

class InnerProject {
    constructor(public name: string, public group: string, public status: StatusEnum) {}
}

@Component({
    selector: 'jhi-migration-detail',
    templateUrl: './migration-detail.component.html',
    styleUrls: ['./migration-detail.component.css']
})
export class MigrationDetailComponent implements OnInit {
    @Input() migration: IMigration;
    @Input() svnModules: string[];
    @Output() startMigration = new EventEmitter<void>();
    @ViewChild('timeline') timeline: DetailsCardComponent;

    private running = StatusEnum.RUNNING;
    private done = StatusEnum.DONE;
    private failed = StatusEnum.FAILED;

    migrationStarted = false;
    projects: InnerProject[] = [];

    constructor(
        private activatedRoute: ActivatedRoute,
        private _migrationService: MigrationService,
        private _gitlabService: MigrationProcessService
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migration, svnModules }) => {
            if (migration !== undefined) {
                this.migration = migration;
            }
        });
        if (this.migration && this.svnModules) {
            this.svnModules.forEach(s => this.initProject(s, this.migration.gitlabGroup));
        }
    }

    initProject(projectName: string, groupName: string) {
        this.projects.push(new InnerProject(projectName, groupName, StatusEnum.RUNNING));
        this._gitlabService.checkProject(`${groupName}${projectName}`, this.migration.gitlabUrl, this.migration.gitlabToken).subscribe(
            res => {
                if (res.body) {
                    this.projects.find(prj => prj.name === projectName).status = StatusEnum.DONE;
                } else {
                    this.projects.find(prj => prj.name === projectName).status = StatusEnum.FAILED;
                }
            },
            _ => (this.projects.find(prj => prj.name === projectName).status = StatusEnum.FAILED)
        );
    }

    getBranchesInfo(): string {
        if (this.migration.branchesToMigrate !== '') {
            return this.migration.branchesToMigrate;
        } else if (this.migration.branches === '*') {
            return '*';
        } else {
            return '';
        }
    }

    getTagsInfo(): string {
        if (this.migration.tagsToMigrate !== '') {
            return this.migration.tagsToMigrate;
        } else if (this.migration.tags === '*') {
            return '*';
        } else {
            return '';
        }
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
