import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MigrationProcessService, SvnModule, SvnStructure } from 'app/migration/migration-process.service';
import { MigrationService } from 'app/entities/migration';
import { IMigration, Migration, MigrationRenaming } from 'app/shared/model/migration.model';
import { IMapping } from 'app/shared/model/mapping.model';
import { SelectionModel } from '@angular/cdk/collections';
import { StaticMappingService } from 'app/entities/static-mapping';
import { ARTIFACTORY_URL, GITLAB_URL, NEXUS_URL, SVN_DEPTH, SVN_URL } from 'app/shared/constants/config.constants';
import { MatCheckboxChange, MatDialog, MatSnackBar, MatSnackBarConfig } from '@angular/material';
import { JhiAddMappingModalComponent } from 'app/migration/add-mapping.component';
import { StaticMapping } from 'app/shared/model/static-mapping.model';
import { TranslateService } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Extension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from 'app/entities/static-extension';
import { ConfigurationService } from 'app/shared/service/configuration-service';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { ActivatedRoute, Router } from '@angular/router';

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
    historyOptionFormGroup: FormGroup;

    // Tables columns
    displayedColumns: string[] = ['delete', 'svn', 'regex', 'git', 'toggleMapping'];
    extensionDisplayedColumns: string[] = ['extensionPattern'];

    // Controls
    gitlabUserKO = null;
    gitlabGroupKO = null;
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
    fileUnit = 'M';
    mig: IMigration;
    svnUrl: string;
    svnCredsOption: string;
    svnDepth: number;
    gitlabUrl: string;
    gitlabUser = '';
    gitlabCredsOption: string;
    renamings: MigrationRenaming[] = [];
    artifactoryUrl: string;
    nexusUrl: string;

    // Cleaning Section
    preserveEmptyDirs = false;

    // Svn selections
    svnSelection: SelectionModel<string>;
    flatRepos = 0;

    // History selections
    historySelection: SelectionModel<string>;
    enableDirectoryFilter = [];
    historyOption = 'nothing';
    svnRevision: string;

    // Mapping selections
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

    // Steps enablement
    stepGitlab;
    stepSvn;
    stepHistory;
    stepCleaning;
    stepMapping;
    stepUpload;

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
        private _configurationService: ConfigurationService,
        private _router: Router,
        private _route: ActivatedRoute
    ) {
        // Init snack bar configuration
        this.snackBarConfig.panelClass = ['errorPanel'];
        this.snackBarConfig.duration = 5000;
        this.snackBarConfig.verticalPosition = 'top';
        this.snackBarConfig.horizontalPosition = 'center';
        this._route.queryParams.subscribe(qp => {
            this.stepGitlab = Boolean(JSON.parse(qp['gitlabEnabled']));
            this.stepSvn = Boolean(JSON.parse(qp['svnEnabled']));
            this.stepHistory = Boolean(JSON.parse(qp['historyEnabled']));
            this.stepMapping = Boolean(JSON.parse(qp['mappingEnabled']));
            this.stepCleaning = Boolean(JSON.parse(qp['cleaningEnabled']));
            if (Boolean(JSON.parse(qp['uploadEnabled']))) {
                this.stepUpload = qp['registry'];
            }
        });
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
            // initial values for apply mapping
            this.initialSelectionMapping = this.mappings.filter(mp => mp.isStatic && !mp.svnDirectoryDelete);
            this.selectionMapping = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelectionMapping);
            // initial values for svnDirectoryDelete
            this.initialSelectionSvnDirectoryDelete = this.mappings.filter(mp => mp.svnDirectoryDelete);
            this.selectionSvnDirectoryDelete = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelectionSvnDirectoryDelete);
        });

        this.gitlabUrl = localStorage.getItem(GITLAB_URL);
        this.svnUrl = localStorage.getItem(SVN_URL);
        this.svnDepth = Number(localStorage.getItem(SVN_DEPTH));
        this.artifactoryUrl = localStorage.getItem(ARTIFACTORY_URL);
        this.nexusUrl = localStorage.getItem(NEXUS_URL);

        this.gitlabFormGroup = this._formBuilder.group({
            gitlabGroup: ['', Validators.required],
            gitlabURL: [{ value: this.gitlabUrl, disabled: true }, Validators.required],
            gitlabToken: ['']
        });
        this.svnFormGroup = this._formBuilder.group({
            svnRepository: ['', Validators.required],
            svnURL: [{ value: this.svnUrl, disabled: true }, Validators.required],
            svnUser: ['', Validators.required],
            svnPwd: ['', Validators.required],
            svnDepth: [this.svnDepth]
        });
        this.svnSelection = new SelectionModel<string>(this.allowMultiSelect, []);
        this.cleaningFormGroup = this._formBuilder.group({
            fileMaxSize: ['100', Validators.min(1)]
        });
        this.mappingFormGroup = this._formBuilder.group({});

        this.historyFormGroup = this._formBuilder.group({
            branchesToMigrate: [''],
            tagsToMigrate: ['']
        });
        this.historyOptionFormGroup = this._formBuilder.group({
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
            .subscribe(res => (this.isGitlabGroupCreation = res), () => (this.isGitlabGroupCreation = false));
    }

    /**
     * Get Gitlab user
     */
    getGitlabUser() {
        // initialise to true for each check
        this.gitlabUserKO = null;
        this.checkingGitlabUser = true;

        this._migrationProcessService
            .currentUserFromToken(this.gitlabFormGroup.controls['gitlabURL'].value, this.gitlabFormGroup.controls['gitlabToken'].value)
            .subscribe(
                res => {
                    console.log(res);
                    this.gitlabUser = res.body;
                    this.gitlabUserKO = false;
                    this.checkingGitlabUser = false;
                },
                err => {
                    console.log(err);
                    this.gitlabUserKO = true;
                    this.checkingGitlabUser = false;
                }
            );
    }

    /**
     * Check if user exists
     */
    checkGitlabUser() {
        // initialise to true for each check
        this.gitlabUserKO = null;

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
                () => {
                    this.gitlabUserKO = true;
                    this.checkingGitlabUser = false;
                }
            );
    }

    /**
     * Check if group exists
     */
    checkGitlabGroup() {
        // initialise to true for each check
        this.gitlabGroupKO = null;

        this.checkingGitlabGroup = true;
        this._migrationProcessService
            .checkGroup(
                this.gitlabFormGroup.controls['gitlabGroup'].value,
                this.gitlabUser,
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
                    this.gitlabGroupKO = true;
                    this.checkingGitlabGroup = false;
                    if (httpER.status === 504) {
                        // this.openSnackBar('error.http.504');
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
                () => {
                    this.creatingGitlabGroup = false;
                    this.forceGitlabGroupCreation = false;
                    this.checkGitlabGroup();
                },
                () => {
                    this.creatingGitlabGroup = false;
                    this.forceGitlabGroupCreation = false;
                    this.openSnackBar('error.creates.gitlab.group');
                }
            );
    }

    /**
     * Load extensions
     */
    loadExtensions() {
        this._extensionsService
            .query({
                name: this.svnFormGroup.value['svnRepository']
            })
            .subscribe(res => {
                this.staticExtensions = res.body as Extension[];
                this.staticExtensions.forEach(ext => (ext.isStatic = true));
                this.extensionSelection = new SelectionModel<Extension>(this.allowMultiSelect, this.staticExtensions);
            });
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
        this.useSvnRootFolder = false;
        this.flatRepo = false;
        this.flatRepos = 0;

        this._migrationProcessService
            .checkSvn(
                this.svnFormGroup.controls['svnRepository'].value,
                this.svnFormGroup.controls['svnURL'].value,
                this.svnFormGroup.controls['svnUser'].value,
                this.svnFormGroup.controls['svnPwd'].value,
                this.svnFormGroup.controls['svnDepth'].value
            )
            .subscribe(
                res => {
                    this.svnDirectories = new SvnStructure(res.body.name, res.body.flat, res.body.root, res.body.uppercase, []);
                    if (res.body.modules && res.body.modules.length > 0) {
                        res.body.modules.forEach(module => this.fillModules(module));
                    } else if (res.body.flat) {
                        if (res.body.root) {
                            this.flatRepo = true;
                        }
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

    removeSelection(module: string) {
        const copy = this.svnSelection.selected;
        this.svnSelection.clear();
        copy.filter(s => s !== module).forEach(m => this.svnSelection.select(m));
    }

    renameModule(module: MigrationRenaming) {
        this.renamings = this.renamings.filter(r => r.oldName !== module.oldName);
        this.renamings.push(module);
    }

    /**
     * Start migration(s)
     */
    async go() {
        const migrationToStart: IMigration[] = [];

        if (this.useSvnRootFolder || (this.svnDirectories && this.svnDirectories.root)) {
            migrationToStart.push(this.initMigration(''));
        } else {
            this.svnSelection.selected.forEach((selection: string) => {
                migrationToStart.push(this.initMigration(selection));
            });
        }

        for (const migration of migrationToStart) {
            await this._migrationService
                .create(migration)
                .toPromise()
                .then(res => {
                    console.log(res);
                });
        }

        this._router.navigate(['/']);
    }

    /**
     * Create migration information from steps
     * @param project
     */
    initMigration(project: string, lastStep = true): IMigration {
        if (!lastStep && this.stepUpload) {
            return;
        }

        if (project === null) {
            if (this.useSvnRootFolder || (this.svnDirectories && this.svnDirectories.root)) {
                project = this.svnFormGroup.controls['svnRepository'].value;
            } else {
                project = this.svnSelection.selected.toString();
            }
        }

        this.mig = new Migration();
        this.mig.trunk = '';
        this.mig.cleaning = this.stepCleaning;

        // Gitlab
        this.mig.gitlabUrl = this.gitlabFormGroup.controls['gitlabURL'].value;
        if (this.gitlabFormGroup.controls['gitlabToken'] !== undefined && this.gitlabFormGroup.controls['gitlabToken'].value !== '') {
            this.mig.gitlabToken = this.gitlabFormGroup.controls['gitlabToken'].value;
        }
        this.mig.user = this.gitlabUser;
        this.mig.gitlabGroup = this.gitlabFormGroup.controls['gitlabGroup'].value;
        const renaming = this.renamings.find(
            r =>
                r.oldName === project ||
                (this.svnDirectories && this.svnDirectories.flat && this.svnFormGroup.controls['svnRepository'].value === r.oldName)
        );
        if (renaming === undefined) {
            this.mig.gitlabProject = project;
        } else {
            this.mig.gitlabProject = renaming.newName;
        }

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

        const module = this.svnDirectories.modules.find(m => m.path === project);

        if (this.svnDirectories.root) this.mig.uppercase = this.svnDirectories.uppercase;
        else this.mig.uppercase = module.uppercase;

        if (this.svnDirectories.modules && this.svnDirectories.modules.length > 0) {
            if (module && module.flat) {
                this.mig.trunk = 'trunk';
                this.mig.flat = true;
            }
        } else {
            this.mig.trunk = 'trunk';
            if (!this.svnDirectories.root) {
                this.mig.flat = this.svnDirectories.flat;
            } else {
                this.mig.flat = false;
            }
        }

        // History
        if (
            this.historySelection !== undefined &&
            !this.historySelection.isEmpty() &&
            ((module && module.layoutElements.length !== 0) || this.svnDirectories.root)
        ) {
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
        if (
            this.historyOptionFormGroup.controls['svnRevision'] !== undefined &&
            this.historyOptionFormGroup.controls['svnRevision'].value !== ''
        ) {
            this.mig.svnRevision = this.historyOptionFormGroup.controls['svnRevision'].value;
        }

        // Branch for master
        if (
            this.historyOptionFormGroup.controls['branchForMaster'] !== undefined &&
            this.historyOptionFormGroup.controls['branchForMaster'].value !== ''
        ) {
            this.mig.trunk = this.historyOptionFormGroup.controls['branchForMaster'].value;
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
        if (this.preserveEmptyDirs !== undefined) {
            this.mig.emptyDirs = this.preserveEmptyDirs;
        } else {
            this.mig.emptyDirs = false;
        }
        if (this.staticExtensions !== undefined && this.staticExtensions.length > 0) {
            const values: string[] = [];
            this.staticExtensions.forEach(ext => values.push(ext.value));
            this.mig.forbiddenFileExtensions = values.toString();
        }

        // Upload
        this.mig.uploadType = this.stepUpload;

        return this.mig;
    }

    /**
     * For Mapping Section
     * Return true if row.svnDirectory has a value
     * @param row
     */
    private isRealMappingRow(row: IMapping) {
        return typeof row.svnDirectory !== 'undefined' && row.svnDirectory !== '';
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
        return (!row.isStatic || (row.isStatic && this.overrideStaticMappings)) && !this.isOriginSvnDeleteDirectory(row);
    }

    /**
     * Delete mapping
     * @param mapping the mapping
     */
    deleteMapping(mapping) {
        this.mappings = this.mappings.filter(m => m !== mapping);
    }

    /**
     * Toggle mapping
     * @param event the toggle event
     */
    toggleMapping(event) {
        if (event.checked) {
            this.toggleMappingEntryType(event['mapping']);
        } else {
            this.toggleSvnDirectoryDeleteType(event['mapping']);
        }
    }

    /**
     * For Mapping Section
     * @param mapping the mapping
     */
    toggleMappingEntryType(mapping: IMapping) {
        if (this.canChangeMappingValue(mapping)) {
            if (this.selectionMapping.isSelected(mapping)) {
                this.selectionMapping.deselect(mapping);
            } else {
                this.selectionMapping.select(mapping);
                // Selecting mapping implicitly means deselecting svnDirectoryDelete
                this.selectionSvnDirectoryDelete.deselect(mapping);
            }
        }
    }

    /**
     * For Mapping Section
     * @param row
     */
    toggleSvnDirectoryDeleteType(row: IMapping) {
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
    isOriginSvnDeleteDirectory(row: IMapping) {
        return (
            (typeof row.gitDirectory === 'undefined' || row.gitDirectory === '') && (typeof row.regex === 'undefined' || row.regex === '')
        );
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
        this.gitlabUserKO = null;
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

    /**
     * Toggle svn directory selection change
     * @param event
     * @param directory
     */
    svnToggle(event: MatCheckboxChange, module: SvnModule) {
        if (event) {
            this.useSvnRootFolder = false;
            if (module.flat) {
                if (event.checked) {
                    this.flatRepos++;
                } else {
                    this.flatRepos--;
                }
            }
            return this.svnSelection.toggle(module.path);
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
        if (this.svnDirectories) {
            return this.svnSelection.selected.length === 0 && !this.useSvnRootFolder && !this.svnDirectories.root;
        }
        return this.svnSelection.selected.length === 0 && !this.useSvnRootFolder;
    }

    getSvnModules(): string[] {
        if (this.svnSelection && this.svnSelection.selected && this.svnSelection.selected.length > 0) {
            return this.svnSelection.selected;
        } else {
            return [this.svnFormGroup.controls['svnRepository'].value];
        }
    }

    isContainsTrunkBranchesTags(directory: SvnModule) {
        return (
            directory != null &&
            directory.layoutElements.length > 0 &&
            directory.layoutElements.includes('trunk') &&
            directory.layoutElements.includes('branches') &&
            directory.layoutElements.includes('tags')
        );
    }

    /**
     * Used in Check your SVN repository section.
     * Returns true if all projects
     */
    isTrunkBranchesTagsEverywhere() {
        return this.svnDirectories.modules.filter(row => !this.isContainsTrunkBranchesTags(row)).length > 0;
    }

    /** Mapping Section : Add a custom mapping. */
    addMapping() {
        const dialog = this._matDialog.open(JhiAddMappingModalComponent, {
            data: { staticMapping: new StaticMapping() }
        });

        dialog.afterClosed().subscribe((result: IMapping) => {
            // better way of checking for undefined
            if (typeof result === 'undefined' || !result) {
                console.log('result undefined: doing nothing');
                return;
            }

            const currentMappings = this.mappings;

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

            // Select either mapping OR svnDirectoryDelete
            if (result.svnDirectoryDelete) {
                this.selectionSvnDirectoryDelete.select(result);
            } else {
                this.selectionMapping.select(result);
            }

            this._changeDetectorRefs.detectChanges();
        });
    }

    /** Root svn directory use selection change. */
    onSelectionChange(event: MatSlideToggleChange) {
        this.useSvnRootFolder = event.checked;
        this.svnSelection.clear();
        this.svnRepoKO = !event.checked;
        event.checked && !this.svnDirectories.root ? (this.flatRepos = 1) : (this.flatRepos = 0);
    }

    /** If loaded extensions are the default ones **/
    isDefaultExtensions(): boolean {
        return !this.staticExtensions.filter((extension: Extension) => extension.name === this.svnFormGroup.value['svnRepository']).length;
    }

    removeExtension(extension: Extension) {
        const copy = this.staticExtensions;
        this.staticExtensions = [];
        copy.filter(e => e.value !== extension.value).forEach(e => this.staticExtensions.push(e));
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
            // this.extensionSelection.toggle(newExtension);
        }
        this.addExtentionFormControl.reset();
    }

    enableHistoryButtons(directory: string) {
        return directory !== 'trunk' && (this.flatRepo || this.svnSelection.selected.length > this.flatRepos);
    }

    /**
     * Toggle svn directory selection change
     * @param directory
     */
    historyToggle(directory: string) {
        this.historySelection.toggle(directory);
        this.historyFormGroup.get(directory + 'ToMigrate').setValue('');
    }

    /**
     * When check/uncheck svn directory
     * @param directory
     */
    historyChecked(directory: string) {
        return this.historySelection.isSelected(directory);
    }

    /**
     * Toggle the directory filter
     * @param directory
     */
    toggleFilter(directory: string): void {
        this.historyFormGroup.get(directory).setValue('');
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     */
    openSnackBar(errorCode: string) {
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }

    /**
     * Cleaning section. Controls switch to on or off. Off/false by default
     */
    togglePreserveEmptyDirs() {
        this.preserveEmptyDirs = !this.preserveEmptyDirs;
    }

    /**
     * Pick CSS class according to module type : flat, classic, error.
     * @param module
     */
    svnFontStyle(module: SvnModule) {
        if (module.layoutElements.length > 0) {
            return 'svn-ok';
        } else if (module.flat) {
            return 'svn-flat';
        } else {
            return 'svn-ko';
        }
    }

    /**
     * Check if selection contains at least on flatRepo
     */
    containsFlatRepo() {
        return !this.flatRepo || this.flatRepos > 0;
    }

    /**
     * Disable element if only flat repos.
     */
    disableWhenOnlyFlat() {
        return (
            this.svnDirectories &&
            ((!this.svnDirectories.root && this.svnDirectories.modules.length === 0) ||
                (this.svnDirectories.modules.length > 0 && this.svnSelection.selected.length === this.flatRepos))
        );
    }

    /**
     * Choose file size unit
     * @param value
     */
    fileSizeUnit(value) {
        this.fileUnit = value.value;
    }

    warningUploadTag() {
        return (
            (this.historyFormGroup.controls['tagsToMigrate'] === undefined ||
                this.historyFormGroup.controls['tagsToMigrate'].value === '') &&
            !this.historySelection.selected.includes('tags')
        );
    }

    warningUploadExtension() {
        return this.staticExtensions.length === 0;
    }
}
