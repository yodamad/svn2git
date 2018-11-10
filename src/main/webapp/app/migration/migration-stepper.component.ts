import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { MigrationService } from 'app/entities/migration';
import { IMigration, Migration } from 'app/shared/model/migration.model';
import { IMapping } from 'app/shared/model/mapping.model';
import { SelectionModel } from '@angular/cdk/collections';
import { StaticMappingService } from 'app/entities/static-mapping';
import { GITLAB_URL, SVN_URL } from 'app/shared/constants/config.constants';

@Component({
    selector: 'jhi-migration-stepper.component',
    templateUrl: 'migration-stepper.component.html',
    styleUrls: ['migration-stepper.component.css']
})
export class MigrationStepperComponent implements OnInit {
    // Form groups
    gitlabFormGroup: FormGroup;
    svnFormGroup: FormGroup;
    cleaningFormGroup: FormGroup;
    mappingFormGroup: FormGroup;
    displayedColumns: string[] = ['svn', 'regex', 'git', 'selectMapping'];

    // Controls
    gitlabUserKO = true;
    gitlabGroupKO = true;
    svnRepoKO = true;
    mappings: IMapping[] = [];
    useDefaultGitlab = true;
    useDefaultSvn = true;

    // Input for migrations
    svnDirectories: string[] = null;
    selectedSvnDirectories: string[];
    selectedExtensions: string[];
    migrationStarted = false;
    fileUnit = 'M';
    mig: IMigration;
    svnUrl: string;
    gitlabUrl: string;
    /// Mapping selections
    initialSelection = [];
    allowMultiSelect = true;
    selection: SelectionModel<IMapping>;

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationProcessService: MigrationProcessService,
        private _migrationService: MigrationService,
        private _mappingService: StaticMappingService
    ) {}

    ngOnInit() {
        this._mappingService.query().subscribe(res => {
            this.mappings = res.body;
            this.initialSelection = this.mappings;
            this.selection = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelection);
        });
        this.gitlabUrl = localStorage.getItem(GITLAB_URL);
        this.svnUrl = localStorage.getItem(SVN_URL);

        this.gitlabFormGroup = this._formBuilder.group({
            gitlabUser: ['', Validators.required],
            gitlabGroup: ['', Validators.required],
            gitlabURL: [{ value: this.gitlabUrl, disabled: true }, Validators.required],
            gitlabToken: ['']
        });
        this.svnFormGroup = this._formBuilder.group({
            svnRepository: ['', Validators.required],
            svnURL: [{ value: this.svnUrl, disabled: true }, Validators.required]
        });
        this.cleaningFormGroup = this._formBuilder.group({
            fileMaxSize: ['', Validators.min(1)]
        });
        this.mappingFormGroup = this._formBuilder.group({});
    }

    /**
     * Check if user exists
     */
    checkGitlabUser() {
        this._migrationProcessService
            .checkUser(
                this.gitlabFormGroup.controls['gitlabUser'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(res => (this.gitlabUserKO = !res.body));
    }

    /**
     * Check if group exists
     */
    checkGitlabGroup() {
        this._migrationProcessService
            .checkGroup(
                this.gitlabFormGroup.controls['gitlabGroup'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(res => (this.gitlabGroupKO = !res.body));
    }

    /**
     * Check if SVN repository exists
     */
    checkSvnRepository() {
        this._migrationProcessService
            .checkSvn(this.svnFormGroup.controls['svnRepository'].value, this.svnFormGroup.controls['svnURL'].value)
            .subscribe(res => {
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
        this.mig.gitlabUrl = this.gitlabFormGroup.controls['gitlabURL'].value;
        if (this.gitlabFormGroup.controls['gitlabToken'] !== undefined && this.gitlabFormGroup.controls['gitlabToken'].value !== '') {
            this.mig.gitlabToken = this.gitlabFormGroup.controls['gitlabToken'].value;
        }
        this.mig.gitlabGroup = this.gitlabFormGroup.controls['gitlabGroup'].value;
        this.mig.gitlabProject = project;
        this.mig.svnUrl = this.svnFormGroup.controls['svnURL'].value;
        this.mig.svnGroup = this.svnFormGroup.controls['svnRepository'].value;
        this.mig.svnProject = project;
        this.mig.user = this.gitlabFormGroup.controls['gitlabUser'].value;
        if (this.cleaningFormGroup.controls['fileMaxSize'] !== undefined) {
            this.mig.maxFileSize = this.cleaningFormGroup.controls['fileMaxSize'].value + this.fileUnit;
        }
        if (this.selectedExtensions !== undefined && this.selectedExtensions.length > 0) {
            this.mig.forbiddenFileExtensions = this.selectedExtensions.toString();
        }
        if (this.selection !== undefined && !this.selection.isEmpty()) {
            this.mig.mappings = this.selection.selected;
        }
        return this.mig;
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSelected() {
        const numSelected = this.selection.selected.length;
        const numRows = this.mappings.length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterToggle() {
        this.isAllSelected() ? this.selection.clear() : this.mappings.forEach(row => this.selection.select(row));
    }

    /** Reverse flag for gitlab default url. */
    reverseGitlab() {
        this.useDefaultGitlab = !this.useDefaultGitlab;
        if (this.useDefaultGitlab) {
            this.gitlabFormGroup.get('gitlabURL').disable();
            this.gitlabFormGroup.get('gitlabURL').setValue(this.gitlabUrl);
            this.gitlabFormGroup.controls['gitlabToken'].reset();
        } else {
            this.gitlabFormGroup.get('gitlabURL').enable();
        }
        // Force recheck
        this.gitlabUserKO = true;
        this.gitlabGroupKO = true;
    }

    /** Reverse flag for svn default url. */
    reverseSvn() {
        this.useDefaultSvn = !this.useDefaultSvn;
        if (this.useDefaultSvn) {
            this.svnFormGroup.get('svnURL').disable();
            this.svnFormGroup.get('svnURL').setValue(this.svnUrl);
        } else {
            this.svnFormGroup.get('svnURL').enable();
        }
        // Force recheck
        this.svnRepoKO = true;
        this.svnDirectories = [];
        this.selectedSvnDirectories = [];
    }
}
