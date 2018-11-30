import { FormGroup, Validators, FormBuilder, FormControl } from '@angular/forms';
import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { MigrationService } from 'app/entities/migration';
import { IMigration, Migration } from 'app/shared/model/migration.model';
import { IMapping, Mapping } from 'app/shared/model/mapping.model';
import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { StaticMappingService } from 'app/entities/static-mapping';
import { GITLAB_URL, SVN_URL } from 'app/shared/constants/config.constants';
import { MatCheckboxChange, MatDialog, MatTableDataSource } from '@angular/material';
import { JhiAddMappingModalComponent } from 'app/migration/add-mapping.component';
import { StaticMapping } from 'app/shared/model/static-mapping.model';

@Component({
    selector: 'jhi-migration-stepper.component',
    templateUrl: 'migration-stepper.component.html',
    styleUrls: ['migration-stepper.component.css']
})
export class MigrationStepperComponent implements OnInit {
    // Static data
    @Input()
    staticExtensions: any[] = [
        { label: '*.zip', value: '*.zip' },
        { label: '*.*ar (including ear, jar, war...)', value: '*.*ar' },
        { label: '*.ear', value: '*.ear' },
        { label: '*.jar', value: '*.jar' },
        { label: '*.war', value: '*.war' },
        { label: '*.tar', value: '*.tar' }
    ];

    ds: DataSource<any> = new MatTableDataSource(this.staticExtensions);

    // Form groups
    gitlabFormGroup: FormGroup;
    svnFormGroup: FormGroup;
    cleaningFormGroup: FormGroup;
    mappingFormGroup: FormGroup;
    addExtentionFormControl: FormControl;

    // Tables columns
    displayedColumns: string[] = ['svn', 'regex', 'git', 'selectMapping'];
    svnDisplayedColumns: string[] = ['svnDir', 'selectSvn'];
    extensionDisplayedColumns: string[] = ['extensionPattern', 'selectExtension'];

    // Controls
    gitlabUserKO = true;
    gitlabGroupKO = true;
    svnRepoKO = true;
    mappings: IMapping[] = [];
    useDefaultGitlab = true;
    useDefaultSvn = true;

    // Input for migrations
    svnDirectories: string[] = null;
    selectedExtensions: any[] = [];
    migrationStarted = false;
    fileUnit = 'M';
    mig: IMigration;
    svnUrl: string;
    gitlabUrl: string;

    /// Svn selections
    svnSelection: SelectionModel<string>;

    /// Mapping selections
    initialSelection = [];
    allowMultiSelect = true;
    selection: SelectionModel<IMapping>;
    useSvnRootFolder = false;

    // Extension selection
    extensionSelection: SelectionModel<any>;

    // Waiting flag
    checkingGitlabUser = false;
    checkingGitlabGroup = false;
    checkingSvnRepo = false;

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationProcessService: MigrationProcessService,
        private _migrationService: MigrationService,
        private _mappingService: StaticMappingService,
        private _matDialog: MatDialog,
        private _changeDetectorRefs: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this._mappingService.query().subscribe(res => {
            this.mappings = res.body;
            this.mappings.push(new Mapping());
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
            svnURL: [{ value: this.svnUrl, disabled: true }, Validators.required],
            svnUser: [''],
            svnPwd: ['']
        });
        this.svnSelection = new SelectionModel<string>(this.allowMultiSelect, []);
        this.cleaningFormGroup = this._formBuilder.group({
            fileMaxSize: ['', Validators.min(1)]
        });
        this.mappingFormGroup = this._formBuilder.group({});

        this.addExtentionFormControl = new FormControl('', []);
        this.extensionSelection = new SelectionModel<string>(this.allowMultiSelect, this.initialSelection);
    }

    /**
     * Check if user exists
     */
    checkGitlabUser() {
        this.checkingGitlabUser = true;
        this._migrationProcessService
            .checkUser(
                this.gitlabFormGroup.controls['gitlabUser'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(res => {
                this.gitlabUserKO = !res.body;
                this.checkingGitlabUser = false;
            }, () => (this.checkingGitlabUser = false));
    }

    /**
     * Check if group exists
     */
    checkGitlabGroup() {
        this.checkingGitlabGroup = true;
        this._migrationProcessService
            .checkGroup(
                this.gitlabFormGroup.controls['gitlabGroup'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(res => {
                this.gitlabGroupKO = !res.body;
                this.checkingGitlabGroup = false;
            }, () => (this.checkingGitlabGroup = false));
    }

    /**
     * Check if SVN repository exists
     */
    checkSvnRepository() {
        this.checkingSvnRepo = true;
        this._migrationProcessService
            .checkSvn(
                this.svnFormGroup.controls['svnRepository'].value,
                this.svnFormGroup.controls['svnURL'].value,
                this.svnFormGroup.controls['svnUser'].value,
                this.svnFormGroup.controls['svnPwd'].value
            )
            .subscribe(res => {
                this.svnDirectories = res.body;
                this.checkingSvnRepo = false;
            }, () => (this.checkingSvnRepo = false));
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

        if (this.useSvnRootFolder) {
            const mig = this.initMigration('');
            this._migrationService.create(mig).subscribe(res => console.log(res));
        } else {
            this.svnSelection.selected
                .map(dir => this.initMigration(dir))
                .forEach(mig => this._migrationService.create(mig).subscribe(res => console.log(res)));
        }
    }

    /**
     * Create migration information from steps
     * @param project
     */
    initMigration(project: string): IMigration {
        if (project === null) {
            if (this.useSvnRootFolder) {
                project = this.svnFormGroup.controls['svnRepository'].value;
            } else {
                project = this.svnSelection.selected.toString();
            }
        }

        this.mig = new Migration();

        // Gitlab
        this.mig.gitlabUrl = this.gitlabFormGroup.controls['gitlabURL'].value;
        if (this.gitlabFormGroup.controls['gitlabToken'] !== undefined && this.gitlabFormGroup.controls['gitlabToken'].value !== '') {
            this.mig.gitlabToken = this.gitlabFormGroup.controls['gitlabToken'].value;
        }
        this.mig.user = this.gitlabFormGroup.controls['gitlabUser'].value;
        this.mig.gitlabGroup = this.gitlabFormGroup.controls['gitlabGroup'].value;
        this.mig.gitlabProject = project;

        // SVN
        this.mig.svnUrl = this.svnFormGroup.controls['svnURL'].value;
        this.mig.svnGroup = this.svnFormGroup.controls['svnRepository'].value;

        this.mig.svnProject = project;
        if (this.svnFormGroup.controls['svnUser'] !== undefined && this.svnFormGroup.controls['svnUser'].value !== '') {
            this.mig.svnUser = this.svnFormGroup.controls['svnUser'].value;
        }
        if (this.svnFormGroup.controls['svnPwd'] !== undefined && this.svnFormGroup.controls['svnPwd'].value !== '') {
            this.mig.svnPassword = this.svnFormGroup.controls['svnPwd'].value;
        }

        // Cleaning
        if (this.cleaningFormGroup.controls['fileMaxSize'] !== undefined) {
            this.mig.maxFileSize = this.cleaningFormGroup.controls['fileMaxSize'].value + this.fileUnit;
        }
        if (this.extensionSelection !== undefined && !this.extensionSelection.isEmpty()) {
            const values: string[] = [];
            this.extensionSelection.selected.forEach(ext => values.push(ext.value));
            this.mig.forbiddenFileExtensions = values.toString();
        }

        // Mappings
        if (this.selection !== undefined && !this.selection.isEmpty()) {
            this.mig.mappings = this.selection.selected.filter(mapping => mapping.gitDirectory !== undefined);
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
            this.svnFormGroup.controls['svnUser'].reset();
            this.svnFormGroup.controls['svnPwd'].reset();
        } else {
            this.svnFormGroup.get('svnURL').enable();
        }
        // Force recheck
        this.svnRepoKO = true;
        this.svnDirectories = [];
        this.svnSelection.clear();
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSvnSelected() {
        const numSelected = this.svnSelection.selected.length;
        const numRows = this.svnDirectories.length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterSvnToggle() {
        if (this.isAllSvnSelected()) {
            this.svnSelection.clear();
            this.svnRepoKO = true;
        } else {
            this.svnDirectories.forEach(row => this.svnSelection.select(row));
            this.useSvnRootFolder = false;
            this.svnRepoKO = false;
        }
    }

    /**
     * Toggle svn directory selection change
     * @param event
     * @param directory
     */
    svnToggle(event: any, directory: string) {
        if (event) {
            this.useSvnRootFolder = false;
            return this.svnSelection.toggle(directory);
        }
        return null;
    }

    /**
     * When check/uncheck svn directory
     * @param directory
     */
    svnChecked(directory: string) {
        this.svnRepoKO = this.svnSelection.selected.length === 0;
        return this.svnSelection.isSelected(directory);
    }

    /** Add a custom mapping. */
    addMapping() {
        const dialog = this._matDialog.open(JhiAddMappingModalComponent, {
            data: { staticMapping: new StaticMapping() }
        });

        const currentMappings = this.mappings;
        // Remove "fake" mapping
        currentMappings.splice(currentMappings.length - 1, 1);

        dialog.afterClosed().subscribe((result: Mapping) => {
            this.mappings = [];
            currentMappings.forEach(mp => this.mappings.push(mp));
            this.mappings.push(result);
            this.mappings.push(new Mapping());
            this.selection.select(result);
            this._changeDetectorRefs.detectChanges();
        });
    }

    /** Root svn directory use selection change. */
    onSelectionChange(event: MatCheckboxChange) {
        this.useSvnRootFolder = event.checked;
        this.svnSelection.clear();
        this.svnRepoKO = !event.checked;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterExtensionToggle() {
        if (this.isAllExtensionSelected()) {
            this.extensionSelection.clear();
        } else {
            this.staticExtensions.forEach(row => this.extensionSelection.select(row));
        }
    }

    /**
     * Toggle extension directory selection change
     * @param event
     * @param extension
     */
    extensionToggle(event: MatCheckboxChange, extension: string) {
        if (event) {
            return this.extensionSelection.toggle(extension);
        }
        return null;
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllExtensionSelected() {
        const numSelected = this.extensionSelection.selected.length;
        const numRows = this.staticExtensions.length;
        return numSelected === numRows;
    }

    /** Add a custom extension. */
    addExtension() {
        console.log(this.addExtentionFormControl.value);
        if (this.addExtentionFormControl.value !== undefined) {
            this.staticExtensions = this.staticExtensions.concat([
                { label: this.addExtentionFormControl.value, value: this.addExtentionFormControl.value }
            ]);
            console.log(this.staticExtensions);
        }
    }
}
