import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MigrationProcessService, SvnFlatModule, SvnModule, SvnStructure } from 'app/migration/migration-process.service';
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
import { Observable, of as observableOf } from 'rxjs';
import { IStaticExtension, StaticExtension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from 'app/entities/static-extension';

@Component({
    selector: 'jhi-migration-stepper.component',
    templateUrl: 'migration-stepper.component.html',
    styleUrls: ['migration-stepper.component.css']
})
export class MigrationStepperComponent implements OnInit {
    // Static data
    staticExtensions: StaticExtension[];
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
    displayedColumns: string[] = ['svn', 'regex', 'git', 'selectMapping'];
    svnDisplayedColumns: string[] = ['selectSvn', 'svnDir'];
    extensionDisplayedColumns: string[] = ['extensionPattern', 'selectExtension'];

    // Controls
    gitlabUserKO = true;
    gitlabGroupKO = true;
    svnRepoKO = true;
    mappings: IMapping[] = [];
    useDefaultGitlab = true;
    useDefaultSvn = true;

    // Input for migrations
    svnDirectories: SvnStructure = null;
    migrationStarted = false;
    fileUnit = 'M';
    mig: IMigration;
    svnUrl: string;
    gitlabUrl: string;

    /// Svn selections
    svnSelection: SelectionModel<string>;

    // History selections
    historySelection: SelectionModel<string>;
    historyOption = 'nothing';

    /// Mapping selections
    initialSelection = [];
    allowMultiSelect = true;
    selection: SelectionModel<IMapping> = new SelectionModel<IMapping>();
    useSvnRootFolder = false;

    // Extension selection
    extensionSelection: SelectionModel<IStaticExtension>;

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
        private _changeDetectorRefs: ChangeDetectorRef,
        private _errorSnackBar: MatSnackBar,
        private _translationService: TranslateService,
        private _extensionsService: StaticExtensionService
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
            this.mappings.push(new Mapping());
            this.initialSelection = this.mappings;
            this.selection = new SelectionModel<IMapping>(this.allowMultiSelect, this.initialSelection);
        });
        this._extensionsService.query().subscribe(res => {
            this.staticExtensions = res.body;
            this.extensionSelection = new SelectionModel<IStaticExtension>(this.allowMultiSelect, this.staticExtensions);
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

        this.historyFormGroup = this._formBuilder.group({});
        this.historySelection = new SelectionModel<string>(this.allowMultiSelect, ['trunk']);

        this.addExtentionFormControl = new FormControl('', []);
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
                        this.openSnackBar('error.checks.gitlab.group');
                    }
                }
            );
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

        // History
        if (this.historySelection !== undefined && !this.historySelection.isEmpty()) {
            this.historySelection.selected.forEach(hst => {
                if (hst === 'trunk') {
                    this.mig.trunk = '*';
                } else if (hst === 'branches') {
                    this.mig.branches = '*';
                } else if (hst === 'tags') {
                    this.mig.tags = '*';
                }
            });
        }
        this.mig.svnHistory = this.historyOption;

        // Mappings
        if (this.selection !== undefined && !this.selection.isEmpty()) {
            this.mig.mappings = this.selection.selected.filter(mapping => mapping.gitDirectory !== undefined);
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
        this.svnDirectories = null;
        this.svnSelection.clear();
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSvnSelected() {
        const numSelected = this.svnSelection.selected.length;
        const numRows = this.svnDirectories.modules.length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterSvnToggle() {
        if (this.isAllSvnSelected()) {
            this.svnSelection.clear();
            this.svnRepoKO = true;
        } else {
            this.svnDirectories.modules.forEach(row => this.svnSelection.select(row.path));
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
            if (result.svnDirectory.startsWith('/')) {
                result.svnDirectory = result.svnDirectory.slice(1, result.svnDirectory.length);
            }
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
    extensionToggle(event: MatCheckboxChange, extension: IStaticExtension) {
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
            this.staticExtensions = this.staticExtensions.concat([
                { value: this.addExtentionFormControl.value, description: this.addExtentionFormControl.value }
            ]);
        }
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
     * @param event
     * @param directory
     */
    historyToggle(event: any, directory: string) {
        if (event) {
            return this.historySelection.toggle(directory);
        }
        return null;
    }

    /**
     * When check/uncheck svn directory
     * @param directory
     */
    historyChecked(directory: string) {
        return this.historySelection.isSelected(directory);
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     */
    openSnackBar(errorCode: string) {
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }

    // SVN Tree elements
    transformer = (node: SvnModule, level: number) => {
        return new SvnFlatModule(node.name, node.path, node.subModules, !!node.subModules, level);
    };

    private _getLevel = (node: SvnFlatModule) => node.level;

    private _isExpandable = (node: SvnFlatModule) => node.expandable;

    private _getChildren = (node: SvnModule): Observable<SvnModule[]> => observableOf(node.subModules);

    hasChild = (_: number, _nodeData: SvnFlatModule) => _nodeData.expandable;
}
