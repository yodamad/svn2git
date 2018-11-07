import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { MigrationService } from 'app/entities/migration';
import { IMigration, Migration } from 'app/shared/model/migration.model';
import { ConfigurationService } from 'app/shared/service/configuration-service';

@Component({
    selector: 'jhi-migration-stepper.component',
    templateUrl: 'migration-stepper.component.html',
    styleUrls: ['migration-stepper.component.css']
})
export class MigrationStepperComponent implements OnInit {
    userFormGroup: FormGroup;
    groupFormGroup: FormGroup;
    svnFormGroup: FormGroup;
    cleaningFormGroup: FormGroup;
    gitlabUserKO = true;
    gitlabGroupKO = true;
    svnRepoKO = true;
    svnDirectories: string[] = null;
    selectedSvnDirectories: string[];
    selectedExtensions: string[];
    migrationStarted = false;
    fileUnit = 'M';
    mig: IMigration;

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationProcessService: MigrationProcessService,
        private _migrationService: MigrationService
    ) {}

    ngOnInit() {
        this.userFormGroup = this._formBuilder.group({
            gitlabUser: ['', Validators.required]
        });
        this.groupFormGroup = this._formBuilder.group({
            gitlabGroup: ['', Validators.required]
        });
        this.svnFormGroup = this._formBuilder.group({
            svnRepository: ['', Validators.required]
        });
        this.cleaningFormGroup = this._formBuilder.group({
            fileMaxSize: ['']
        });
    }

    /**
     * Check if user exists
     */
    checkGitlabUser() {
        this._migrationProcessService
            .checkUser(this.userFormGroup.controls['gitlabUser'].value)
            .subscribe(res => (this.gitlabUserKO = !res.body));
    }

    /**
     * Check if group exists
     */
    checkGitlabGroup() {
        this._migrationProcessService
            .checkGroup(this.groupFormGroup.controls['gitlabGroup'].value)
            .subscribe(res => (this.gitlabGroupKO = !res.body));
    }

    /**
     * Check if SVN repository exists
     */
    checkSvnRepository() {
        this._migrationProcessService.checkSvn(this.svnFormGroup.controls['svnRepository'].value).subscribe(res => {
            this.svnDirectories = res.body;
        });
    }

    /**
     * Get selected projects to migrate
     * @param values
     */
    onSelectedOptionsChange(values: string[]) {
        this.selectedSvnDirectories = values;
        this.svnRepoKO = this.selectedSvnDirectories.length === 0;
    }

    /**
     * Get selected extensions to clean
     * @param values
     */
    onSelectedExtensionsChange(values: string[]) {
        this.selectedExtensions = values;
    }

    /**
     * Choose file size unit
     * @param value
     */
    fileSizeUnit(value) {
        this.fileUnit = value.value;
    }

    /**
     * Dynamically set css class on Check button
     * @param flag
     */
    cssClass(flag: string) {
        return flag ? 'ko' : 'ok';
    }

    /**
     * Start migration(s)
     */
    go() {
        this.migrationStarted = true;
        this.selectedSvnDirectories
            .map(dir => this.initMigration(dir))
            .forEach(mig => this._migrationService.create(mig).subscribe(res => console.log(res)));
    }

    /**
     * Create migration information from steps
     * @param project
     */
    initMigration(project: string): IMigration {
        if (project === null) {
            project = this.selectedSvnDirectories.toString();
        }

        this.mig = new Migration();
        this.mig.gitlabGroup = this.groupFormGroup.controls['gitlabGroup'].value;
        this.mig.gitlabProject = project;
        this.mig.svnGroup = this.svnFormGroup.controls['svnRepository'].value;
        this.mig.svnProject = project;
        this.mig.user = this.userFormGroup.controls['gitlabUser'].value;
        if (this.cleaningFormGroup.controls['fileMaxSize'] !== undefined) {
            this.mig.maxFileSize = this.cleaningFormGroup.controls['fileMaxSize'].value + this.fileUnit;
        }
        if (this.selectedExtensions !== undefined && this.selectedExtensions.length > 0) {
            this.mig.forbiddenFileExtensions = this.selectedExtensions.toString();
        }
        return this.mig;
    }
}
