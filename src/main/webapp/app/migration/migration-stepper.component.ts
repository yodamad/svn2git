import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MigrationProcessService, SvnModule, SvnStructure } from 'app/migration/migration-process.service';
import { MigrationService } from 'app/entities/migration';
import { IMigration, Migration } from 'app/shared/model/migration.model';
import { IMapping, Mapping } from 'app/shared/model/mapping.model';
import { SelectionModel } from '@angular/cdk/collections';
import { StaticMappingService } from 'app/entities/static-mapping';
import { GITLAB_URL, SVN_URL } from 'app/shared/constants/config.constants';
import { MatCheckboxChange, MatDialog, MatSnackBar, MatSnackBarConfig } from '@angular/material';
import { JhiAddMappingModalComponent } from 'app/migration/add-mapping.component';
import { StaticMapping } from 'app/shared/model/static-mapping.model';
import { TranslateService } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Extension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from 'app/entities/static-extension';
import { ConfigurationService } from 'app/shared/service/configuration-service';

export const REQUIRED = 'required';

@Component({
    selector: 'jhi-migration-stepper.component',
    templateUrl: 'migration-stepper.component.html',
    styleUrls: ['migration-stepper.component.css']
})
export class MigrationStepperComponent implements OnInit {
    // Static data
    staticExtensions: Extension[] = [];
    staticDirectories: string[] = ['trunk', 'branches', 'tags'];

    // SnackBar config
    snackBarConfig = new MatSnackBarConfig();

    // Form groups
    gitlabFormGroup: FormGroup;
    svnFormGroup: FormGroup;
    cleaningFormGroup: FormGroup;
    mappingFormGroup: FormGroup;
    addExtentionFormControl: FormControl;
    historyFormGroup: FormGroup;

    // Tables columns
    displayedColumns: string[] = ['svn', 'regex', 'git', 'selectMapping', 'svnDirectoryDelete'];
    svnDisplayedColumns: string[] = ['selectSvn', 'svnDir'];
    extensionDisplayedColumns: string[] = ['extensionPattern', 'selectExtension'];

    // Controls
    gitlabUserKO = true;
    gitlabGroupKO = true;
    svnRepoKO = true;
    mappings: IMapping[] = [];
    useDefaultGitlab = true;
    overrideStaticExtensions = false;
    overrideStaticMappings = false;
    forceGitlabGroupCreation = false;
    useDefaultSvn = true;
    svnUrlModifiable = true;
    flatRepo = false;

    // Input for migrations
    svnDirectories: SvnStructure = null;
    migrationStarted = false;
    fileUnit = 'M';
    mig: IMigration;
    svnUrl: string;
    svnCredsOption: string;
    gitlabUrl: string;
    gitlabCredsOption: string;

    /// Svn selections
    svnSelection: SelectionModel<string>;

    // History selections
    historySelection: SelectionModel<string>;
    historyOption = 'nothing';
    svnRevision: string;

    /// Mapping selections
    initialSelectionMapping = [];
    allowMultiSelect = true;
    selectionMapping: SelectionModel<IMapping> = new SelectionModel<IMapping>();
    initialSelectionSvnDirectoryDelete = [];
    selectionSvnDirectoryDelete: SelectionModel<IMapping> = new SelectionModel<IMapping>();
    useSvnRootFolder = false;

    // Extension selection
    extensionSelection: SelectionModel<Extension> = new SelectionModel<Extension>();

    // Waiting flag
    checkingGitlabUser = false;
    checkingGitlabGroup = false;
    checkingSvnRepo = false;
    creatingGitlabGroup = false;

    // Flag : Allow application to automatically make non existing Group
    isGitlabGroupCreation = false;

    constructor(
        private _formBuilder: FormBuilder,
        private _migrationProcessService: MigrationProcessService,
        private _migrationService: MigrationService,
        private _mappingService: StaticMappingService,
        private _matDialog: MatDialog,
        private _changeDetectorRefs: ChangeDetectorRef,
        private _errorSnackBar: MatSnackBar,
        private _translationService: TranslateService,
        private _extensionsService: StaticExtensionService,
        private _configurationService: ConfigurationService
    ) {
        // Init snack bar configuration
        this.snackBarConfig.panelClass = ['errorPanel'];
        this.snackBarConfig.duration = 5000;
        this.snackBarConfig.verticalPosition = 'top';
        this.snackBarConfig.horizontalPosition = 'center';
    }

    ngOnInit() {
        this._mappingService.query().subscribe(res => {
            this.mappings = res.body;
            // everything from the database is flagged with isStatic
            this.mappings.forEach(mp => {
                // if (!mp.svnDirectoryDelete) {
                // set isStatic value
                mp.isStatic = true;
                // }
            });
            this.mappings.push(new Mapping());
            // initial values for apply mapping
            this.initialSelectionMapping = this.mappings.filter(mp => mp.isStatic && !mp.svnDirectoryDelete);
            this.selectionMapping = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelectionMapping);
            // initial values for svnDirectoryDelete
            this.initialSelectionSvnDirectoryDelete = this.mappings.filter(mp => mp.svnDirectoryDelete);
            this.selectionSvnDirectoryDelete = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelectionSvnDirectoryDelete);
        });
        this._extensionsService.query().subscribe(res => {
            this.staticExtensions = res.body as Extension[];
            this.staticExtensions.forEach(ext => (ext.isStatic = true));
            this.extensionSelection = new SelectionModel<Extension>(this.allowMultiSelect, this.staticExtensions);
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
            fileMaxSize: ['100', Validators.min(1)]
        });
        this.mappingFormGroup = this._formBuilder.group({});

        this.historyFormGroup = this._formBuilder.group({
            branchesToMigrate: [''],
            tagsToMigrate: [''],
            branchForMaster: [''],
            svnRevision: ['']
        });
        this.historySelection = new SelectionModel<string>(this.allowMultiSelect, ['trunk']);

        this.addExtentionFormControl = new FormControl('', []);

        this._configurationService.overrideStaticExtensions().subscribe(res => (this.overrideStaticExtensions = res));
        this._configurationService.overrideStaticMappings().subscribe(res => (this.overrideStaticMappings = res));
        this._configurationService.gitlabCredsOption().subscribe(res => {
            this.gitlabCredsOption = res;
            this.useDefaultGitlab = res !== REQUIRED;
            if (!this.useDefaultGitlab) {
                this.gitlabFormGroup.get('gitlabURL').enable();
            }
        });
        this._configurationService.svnCredsOption().subscribe(res => {
            // a value of svnCredsOption 'required' results in necessity to enter credentials
            this.svnCredsOption = res;
        });

        this._configurationService.svnUrlModifiable().subscribe((res: boolean) => {
            // real boolean value see JSON.parse in svnUrlModifiable
            this.svnUrlModifiable = res;
        });

        this._configurationService
            .flagGitlabGroupCreation()
            .subscribe(res => (this.isGitlabGroupCreation = res), err => (this.isGitlabGroupCreation = false));
    }

    /**
     * Check if user exists
     */
    checkGitlabUser() {
        // initialise to true for each check
        this.gitlabUserKO = true;

        this.checkingGitlabUser = true;
        this._migrationProcessService
            .checkUser(
                this.gitlabFormGroup.controls['gitlabUser'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(
                res => {
                    this.gitlabUserKO = !res.body;
                    this.checkingGitlabUser = false;
                },
                error => {
                    const httpER: HttpErrorResponse = error;
                    this.checkingGitlabUser = false;
                    if (httpER.status === 504) {
                        this.openSnackBar('error.http.504');
                    } else {
                        this.openSnackBar('error.checks.gitlab.user');
                    }
                }
            );
    }

    /**
     * Check if group exists
     */
    checkGitlabGroup() {
        // initialise to true for each check
        this.gitlabGroupKO = true;

        this.checkingGitlabGroup = true;
        this._migrationProcessService
            .checkGroup(
                this.gitlabFormGroup.controls['gitlabGroup'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(
                res => {
                    this.gitlabGroupKO = !res.body;
                    this.checkingGitlabGroup = false;
                },
                error => {
                    const httpER: HttpErrorResponse = error;
                    this.checkingGitlabGroup = false;
                    if (httpER.status === 504) {
                        this.openSnackBar('error.http.504');
                    } else {
                        if (this.isGitlabGroupCreation) {
                            // Users will click a button to create the group
                            this.openSnackBar('error.checks.gitlab.groupauto');
                            this.forceGitlabGroupCreation = true;
                        } else {
                            // Users will be aske to creat a group manually
                            this.openSnackBar('error.checks.gitlab.groupmanual');
                            this.forceGitlabGroupCreation = false;
                        }
                    }
                }
            );
    }

    /**
     * Create group in gitlab
     */
    createGitlabGroup() {
        this.creatingGitlabGroup = true;
        this._migrationProcessService
            .createGroup(
                this.gitlabFormGroup.controls['gitlabGroup'].value,
                this.gitlabFormGroup.controls['gitlabURL'].value,
                this.gitlabFormGroup.controls['gitlabToken'].value
            )
            .subscribe(
                res => {
                    this.creatingGitlabGroup = false;
                    this.forceGitlabGroupCreation = false;
                    this.checkGitlabGroup();
                },
                error => {
                    this.creatingGitlabGroup = false;
                    this.forceGitlabGroupCreation = false;
                    this.openSnackBar('error.creates.gitlab.group');
                }
            );
    }

    /**
     * Check if SVN repository exists
     */
    checkSvnRepository() {
        // Force recheck
        this.svnRepoKO = true;
        this.svnDirectories = null;
        this.svnSelection.clear();

        this.checkingSvnRepo = true;
        this._migrationProcessService
            .checkSvn(
                this.svnFormGroup.controls['svnRepository'].value,
                this.svnFormGroup.controls['svnURL'].value,
                this.svnFormGroup.controls['svnUser'].value,
                this.svnFormGroup.controls['svnPwd'].value
            )
            .subscribe(
                res => {
                    if (res.body.modules) {
                        this.svnDirectories = new SvnStructure(res.body.name, res.body.flat, []);
                        res.body.modules.forEach(module => this.fillModules(module));
                    } else if (res.body.flat) {
                        this.useSvnRootFolder = true;
                    }

                    this.checkingSvnRepo = false;
                },
                error => {
                    const httpER: HttpErrorResponse = error;
                    this.checkingSvnRepo = false;
                    if (httpER.status === 504) {
                        this.openSnackBar('error.http.504');
                    } else {
                        this.openSnackBar('error.checks.svn');
                    }
                }
            );
    }

    /**
     * Recurvice inspection of submodules
     * @param module
     */
    fillModules(module: SvnModule) {
        console.log('Inspecting ' + module.name);
        if (module.subModules.length > 0) {
            console.log(module.name + ' has submodules');
            module.subModules.forEach(submodule => this.fillModules(submodule));
        } else {
            this.svnDirectories.modules.push(module);
        }
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
    cssClass(flag: boolean) {
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
        this.mig.trunk = '';

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

        this.mig.flat = this.flatRepo;
        if (this.flatRepo) {
            this.mig.trunk = 'trunk';
        }

        // History
        if (this.historySelection !== undefined && !this.historySelection.isEmpty()) {
            this.historySelection.selected.forEach(hst => {
                if (hst === 'trunk') {
                    this.mig.trunk = 'trunk';
                } else if (hst === 'branches') {
                    this.mig.branches = '*';
                } else if (hst === 'tags') {
                    this.mig.tags = '*';
                }
            });

            if (
                this.historyFormGroup.controls['branchesToMigrate'] !== undefined &&
                this.historyFormGroup.controls['branchesToMigrate'].value !== ''
            ) {
                this.mig.branchesToMigrate = this.historyFormGroup.controls['branchesToMigrate'].value;
            } else {
                this.mig.branchesToMigrate = '';
            }

            if (
                this.historyFormGroup.controls['tagsToMigrate'] !== undefined &&
                this.historyFormGroup.controls['tagsToMigrate'].value !== ''
            ) {
                this.mig.tagsToMigrate = this.historyFormGroup.controls['tagsToMigrate'].value;
            } else {
                this.mig.tagsToMigrate = '';
            }
        }
        this.mig.svnHistory = this.historyOption;

        // Revision to start
        if (this.historyFormGroup.controls['svnRevision'] !== undefined && this.historyFormGroup.controls['svnRevision'].value !== '') {
            this.mig.svnRevision = this.historyFormGroup.controls['svnRevision'].value;
        }

        // Branch for master
        if (
            this.historyFormGroup.controls['branchForMaster'] !== undefined &&
            this.historyFormGroup.controls['branchForMaster'].value !== ''
        ) {
            this.mig.trunk = this.historyFormGroup.controls['branchForMaster'].value;
        }

        // Mappings
        // Note : selectionSvnDirectoryDelete can be empty
        if (this.selectionMapping !== undefined && !this.selectionMapping.isEmpty() && this.selectionSvnDirectoryDelete !== undefined) {
            // Assure mappings.svnDirectoryDelete is updated from selectionSvnDirectoryDelete
            this.mappings.forEach(row => {
                if (this.selectionSvnDirectoryDelete.isSelected(row)) {
                    row.svnDirectoryDelete = true;
                    // svnDirectoryDelete assure that no gitDirectory or regex.
                    row.gitDirectory = '';
                    row.regex = '';
                } else {
                    row.svnDirectoryDelete = false;
                }
            });

            // this.mig.mappings = this.selectionMapping.selected
            //    .filter(mapping => mapping.gitDirectory !== undefined);
            this.mig.mappings = this.mappings.filter(row => {
                return (
                    (this.isRealMappingRow(row) && this.selectionMapping.isSelected(row)) ||
                    this.selectionSvnDirectoryDelete.isSelected(row)
                );
            });
        }

        // Cleaning
        if (this.cleaningFormGroup.controls['fileMaxSize'] !== undefined) {
            this.mig.maxFileSize = this.cleaningFormGroup.controls['fileMaxSize'].value + this.fileUnit;
            console.log(this.mig.maxFileSize);
        }
        if (this.extensionSelection !== undefined && !this.extensionSelection.isEmpty()) {
            const values: string[] = [];
            this.extensionSelection.selected.forEach(ext => values.push(ext.value));
            this.mig.forbiddenFileExtensions = values.toString();
        }

        return this.mig;
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSelectedMapping() {
        const numSelected = this.mappings.filter(
            row => this.isRealMappingRow(row) && !row.isStatic && this.selectionMapping.isSelected(row)
        ).length;
        const numRows = this.mappings.filter(row => this.isRealMappingRow(row) && !row.isStatic && !this.isOriginSvnDeleteDirectory(row))
            .length;
        return numSelected === numRows;
    }

    /** svnDirectoryDelete : Whether the number of selected elements matches the total number of rows. */
    isAllSelectedSvnDirectoryDelete() {
        const numSelected = this.mappings.filter(
            row => this.isRealMappingRow(row) && !row.isStatic && this.selectionSvnDirectoryDelete.isSelected(row)
        ).length;
        const numRows = this.mappings.filter(row => this.isRealMappingRow(row) && !row.isStatic).length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterToggleMapping() {
        // if all mappings are already selected we will deselect where appropriate
        if (this.isAllSelectedMapping()) {
            // iterate over all rows
            this.mappings.forEach(row => {
                // only consider real rows (i.e. not the last dummy row)
                if (this.isRealMappingRow(row)) {
                    // If canChangeMappingValue we deselect it
                    if (this.canChangeMappingValue(row)) {
                        this.selectionMapping.deselect(row);

                        // don't do anything with svnDirectoryDelete. might just want to stop mapping
                    }
                }
            });
        } else {
            this.mappings.forEach(row => {
                // only consider real rows (i.e. not the last dummy row)
                if (this.isRealMappingRow(row)) {
                    // Select mapping if possible
                    if (this.canChangeMappingValue(row)) {
                        this.selectionMapping.select(row);

                        // if we are selecting a mapping, we implicitly mean that svn delete
                        // directory will be deselected
                        this.selectionSvnDirectoryDelete.deselect(row);
                    }
                }
            });
        }
    }

    /**
     * For Mapping Section
     * Return true if row.svnDirectory has a value
     * @param row
     */
    private isRealMappingRow(row: IMapping) {
        if (typeof row.svnDirectory !== 'undefined' && row.svnDirectory !== '') {
            return true;
        }
        return false;
    }

    /**
     * For Mapping Section
     * Return true
     * if row isn't static OR is static but can be overridden
     * AND
     * wasn't created to delete a subversion directory
     *
     * @param row
     */
    private canChangeMappingValue(row: IMapping) {
        if ((!row.isStatic || (row.isStatic && this.overrideStaticMappings)) && !this.isOriginSvnDeleteDirectory(row)) {
            return true;
        }

        return false;
    }

    /** svnDirectoryDelete : Selects all rows if they are not all selected; otherwise clear selection. */
    masterToggleSvnDirectoryDelete() {
        // if all svnDeleteDirectory selected we deselect
        if (this.isAllSelectedSvnDirectoryDelete()) {
            // iterate over all rows
            this.mappings.forEach(row => {
                // only consider real rows
                if (this.isRealMappingRow(row) && !row.isStatic) {
                    // deselect the svnDirectoryDelete (no conditions)
                    this.selectionSvnDirectoryDelete.deselect(row);

                    // don't do anything to mapping value
                }
            });
        } else {
            this.mappings.forEach(row => {
                // only consider real rows
                if (this.isRealMappingRow(row) && !row.isStatic) {
                    // select the svnDirectoryDelete (no conditions)
                    this.selectionSvnDirectoryDelete.select(row);

                    // selecting svnDirectoryDelete implicitly means mapping is removed where possible
                    if (this.canChangeMappingValue(row)) {
                        this.selectionMapping.deselect(row);
                    }
                }
            });
        }
    }

    /**
     * For Mapping Section
     * @param row
     */
    toggleMappingEntryType(row: Mapping) {
        if (this.canChangeMappingValue(row)) {
            if (this.selectionMapping.isSelected(row)) {
                this.selectionMapping.deselect(row);
            } else {
                this.selectionMapping.select(row);
                // Selecting mapping implicitly means deselecting svnDirectoryDelete
                this.selectionSvnDirectoryDelete.deselect(row);
            }
        }
    }

    /**
     * For Mapping Section
     * @param row
     */
    toggleSvnDirectoryDeleteType(row: Mapping) {
        if (this.selectionSvnDirectoryDelete.isSelected(row)) {
            this.selectionSvnDirectoryDelete.deselect(row);
        } else {
            this.selectionSvnDirectoryDelete.select(row);
            // Selecting svnDirectoryDelete implicitly means deselecting mapping
            if (this.canChangeMappingValue(row)) {
                this.selectionMapping.deselect(row);
            }
        }
    }

    /**
     * For Mapping Section
     * @param row
     */
    isOriginSvnDeleteDirectory(row: Mapping) {
        if (
            (typeof row.gitDirectory === 'undefined' || row.gitDirectory === '') &&
            (typeof row.regex === 'undefined' || row.regex === '')
        ) {
            return true;
        }

        return false;
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

    /** Reverse flag for svn default url. can only be invoked if svnUrlModifiable in config */
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
        this.svnDirectories = null;
        this.svnSelection.clear();
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSvnSelected() {
        const numSelected = this.svnSelection.selected.length;
        const numRows = this.svnDirectories.modules.filter(row => !this.isSvnOnlyTags(row)).length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterSvnToggle() {
        if (this.isAllSvnSelected()) {
            this.svnSelection.clear();
            this.svnRepoKO = true;
        } else {
            this.svnDirectories.modules.filter(row => !this.isSvnOnlyTags(row)).forEach(row => this.svnSelection.select(row.path));
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
        return this.svnSelection.isSelected(directory);
    }

    getSvnRepoKo(): boolean {
        return this.svnSelection.selected.length === 0;
    }

    /**
     * SVN section. If directory only contains tags layoutElement it is considered as a composite project
     * (used to group together other tags). Projects like these are not migrated.
     * @param directory
     */
    isSvnOnlyTags(directory: SvnModule) {
        if (directory != null && directory.layoutElements.length === 1 && directory.layoutElements[0] === 'tags') {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Used in Chyeck your SVN repository section.
     * Returns true if at least one of the Directories has a tags only layout
     */
    isOneSvnOnlyTags() {
        return this.svnDirectories.modules.filter(row => this.isSvnOnlyTags(row)).length > 0;
    }

    /** Mapping Section : Add a custom mapping. */
    addMapping() {
        const dialog = this._matDialog.open(JhiAddMappingModalComponent, {
            data: { staticMapping: new StaticMapping() }
        });

        dialog.afterClosed().subscribe((result: Mapping) => {
            // better way of checking for undefined
            if (typeof result === 'undefined') {
                console.log('result undefined: doing nothing');
            }
            {
                const currentMappings = this.mappings;
                // Remove "fake" mapping
                currentMappings.splice(currentMappings.length - 1, 1);

                // remove any initial forward slash
                if (result.svnDirectory && result.svnDirectory.startsWith('/')) {
                    result.svnDirectory = result.svnDirectory.slice(1, result.svnDirectory.length);
                }

                // delete mappings
                this.mappings = [];
                // recreate mappings cleanly
                currentMappings.forEach(mp => this.mappings.push(mp));
                // add the new mapping
                this.mappings.push(result);
                // add a dummy mapping (last row)
                this.mappings.push(new Mapping());

                // Select either mapping OR svnDirectoryDelete
                if (result.svnDirectoryDelete) {
                    this.selectionSvnDirectoryDelete.select(result);
                } else {
                    this.selectionMapping.select(result);
                }

                this._changeDetectorRefs.detectChanges();
            }
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
            if (!this.overrideStaticExtensions) {
                const staticExts: Extension[] = this.staticExtensions.filter(ext => ext.isStatic);
                staticExts.forEach(row => this.extensionSelection.select(row));
            }
        } else {
            this.staticExtensions.forEach(row => this.extensionSelection.select(row));
        }
    }

    /**
     * Toggle extension directory selection change
     * @param event
     * @param extension
     */
    extensionToggle(event: MatCheckboxChange, extension: Extension) {
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
        if (this.addExtentionFormControl.value !== undefined && this.addExtentionFormControl.value !== '') {
            const newExtension: Extension = {
                value: this.addExtentionFormControl.value,
                description: this.addExtentionFormControl.value,
                isStatic: false
            };
            this.staticExtensions = this.staticExtensions.concat([newExtension]);
            this.extensionSelection.select(newExtension);
        }
        this.addExtentionFormControl.reset();
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllHistoriesSelected() {
        const numSelected = this.historySelection.selected.length;
        const numRows = this.staticDirectories.length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterHistoryToggle() {
        if (this.isAllHistoriesSelected()) {
            this.historySelection.clear();
        } else {
            this.staticDirectories.forEach(row => this.historySelection.select(row));
        }
    }

    /**
     * Toggle svn directory selection change
     * @param directory
     */
    historyToggle(directory: string) {
        this.historySelection.toggle(directory);

        if (this.isSetBranchesTags()) {
            if (directory === 'branches') {
                if (this.historySelection.isSelected(directory)) {
                    this.historyFormGroup.get('branchesToMigrate').enable();
                } else {
                    this.historyFormGroup.get('branchesToMigrate').setValue('');
                    this.historyFormGroup.get('branchesToMigrate').disable();
                }
            }

            if (directory === 'tags') {
                if (this.historySelection.isSelected(directory)) {
                    this.historyFormGroup.get('tagsToMigrate').enable();
                } else {
                    this.historyFormGroup.get('tagsToMigrate').setValue('');
                    this.historyFormGroup.get('tagsToMigrate').disable();
                }
            }
        }
    }

    /**
     * When check/uncheck svn directory
     * @param directory
     */
    historyChecked(directory: string) {
        if (directory === 'trunk') {
            return this.historySelection.isSelected(directory) && !this.noTrunk();
        }
        return this.historySelection.isSelected(directory);
    }

    /**
     * triggered when moving on to historyStep from svn step
     */
    historyStepSetDisabledBranchesTags() {
        if (this.isSetBranchesTags()) {
            if (this.historySelection.isSelected('branches')) {
                this.historyFormGroup.get('branchesToMigrate').enable();
            } else {
                this.historyFormGroup.get('branchesToMigrate').disable();
            }

            if (this.historySelection.isSelected('tags')) {
                this.historyFormGroup.get('tagsToMigrate').enable();
            } else {
                this.historyFormGroup.get('tagsToMigrate').disable();
            }
        }
    }

    /**
     * history step
     */
    isSetBranchesTags() {
        if ((this.svnSelection != null && this.svnSelection.selected.length > 1) || this.useSvnRootFolder) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     */
    openSnackBar(errorCode: string) {
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }

    isSvnLayoutDisabled(svnLayout: string) {
        if (this.historyOption === 'nothing' && (svnLayout === 'branches' || svnLayout === 'tags')) {
            return true;
        } else {
            return false;
        }
    }

    noTrunk() {
        return (
            this.historyFormGroup.controls['branchForMaster'] !== undefined &&
            this.historyFormGroup.controls['branchForMaster'].value !== ''
        );
    }

    noStdLayout() {
        return this.flatRepo;
    }

    flatManager() {
        this.flatRepo = !this.flatRepo;

        if (this.flatRepo) {
            this.historySelection.clear();
            this.historyFormGroup.get('branchesToMigrate').disable();
            this.historyFormGroup.get('branchesToMigrate').setValue('');
            this.historyFormGroup.get('tagsToMigrate').disable();
            this.historyFormGroup.get('tagsToMigrate').setValue('');
            this.historyFormGroup.controls['branchForMaster'].disable();
            this.historyFormGroup.controls['branchForMaster'].setValue('');
        } else {
            this.historyFormGroup.get('branchesToMigrate').enable();
            this.historyFormGroup.get('tagsToMigrate').enable();
            this.historyFormGroup.controls['branchForMaster'].enable();
        }
    }

    svnFontStyle(module: SvnModule) {
        if (module.layoutElements.length > 0) {
            return 'svn-ok';
        } else if (module.isFlat) {
            return 'svn-flat';
        } else {
            return 'svn-ko';
        }
    }
}
