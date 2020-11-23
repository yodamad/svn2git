import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigration, MigrationRenaming, StatusEnum } from 'app/shared/model/migration.model';
import { MigrationService } from 'app/entities/migration/migration.service';
import { DetailsCardComponent } from 'app/shared/summary/summary-details.component';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { FormControl, Validators } from '@angular/forms';

class InnerProject {
    constructor(public name: string, public group: string, public status: StatusEnum, public rename = '') {}
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
    @Output() removeMigration = new EventEmitter<string>();
    @Output() rename = new EventEmitter<MigrationRenaming>();
    @ViewChild('timeline') timeline: DetailsCardComponent;

    private running = StatusEnum.RUNNING;
    private done = StatusEnum.DONE;
    private failed = StatusEnum.FAILED;

    migrationStarted = false;
    projects: InnerProject[] = [];
    newName: FormControl;
    editing = '';

    constructor(
        private activatedRoute: ActivatedRoute,
        private _migrationService: MigrationService,
        private _gitlabService: MigrationProcessService
    ) {}

    ngOnInit() {
        this.newName = new FormControl('/', Validators.required);
        this.activatedRoute.data.subscribe(({ migration, svnModules }) => {
            if (migration !== undefined) {
                this.migration = migration;
            }
        });
        if (this.migration && this.svnModules) {
            this.svnModules.forEach(s => this.initProject(s));
        }
    }

    initProject(projectName: string) {
        this.projects.push(new InnerProject(projectName, this.migration.gitlabGroup, StatusEnum.RUNNING));
        this.checkProject(projectName);
    }

    checkProject(projectName: string, oldName = '') {
        this._gitlabService
            .checkProject(`${this.migration.gitlabGroup}${projectName}`, this.migration.gitlabUrl, this.migration.gitlabToken)
            .subscribe(
                res => {
                    if (res.body) {
                        this.projects.find(prj => prj.name === projectName || prj.rename === projectName).status = StatusEnum.DONE;
                        if (oldName !== '') {
                            this.rename.emit(new MigrationRenaming(oldName, projectName));
                        }
                    } else {
                        const project = this.projects.find(prj => prj.name === projectName || prj.rename === projectName);
                        project.status = StatusEnum.FAILED;
                        if (oldName !== '') {
                            project.rename = '';
                        }
                    }
                },
                _ => {
                    const project = this.projects.find(prj => prj.name === projectName || prj.rename === projectName);
                    project.status = StatusEnum.FAILED;
                    if (oldName !== '') {
                        project.rename = '';
                    }
                }
            );
    }

    getProjectName(project: InnerProject): string {
        if (project.rename !== '') {
            return project.rename;
        }
        return project.name;
    }

    removeProject(project: string) {
        this.removeMigration.emit(project);
        this.projects = this.projects.filter(p => p.name !== project);
        this.svnModules = this.svnModules.filter(p => p !== project);
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

    migrationNotPossible(): boolean {
        return this.projects && this.projects.find(p => p.status === StatusEnum.FAILED) !== undefined;
    }

    edit(project: string) {
        this.editing = project;
    }

    renameMigration(old: string) {
        this.projects.find(p => p.name === old).rename = this.newName.value;
        console.log(new MigrationRenaming(old, this.newName.value));
        this.checkProject(this.newName.value, old);
        this.editing = '';
        this.newName.setValue('/');
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
