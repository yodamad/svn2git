import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { Router } from '@angular/router';
import { ConfigurationService } from 'app/shared/service/configuration-service';
import { IMigration, Migration, StatusEnum } from 'app/shared/model/migration.model';
import { HttpResponse } from '@angular/common/http';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-home',
    templateUrl: './home.component.html',
    styleUrls: ['home.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
    // Migration refresh
    static migrationRefreshInterval = 20000; // ms
    migrationRefreshTimer;

    account: Account;
    modalRef: NgbModalRef;

    // migrations in Running or Waiting State initialised in ngOnInit
    migrations: IMigration[] = [];

    // SnackBar config
    snackBarConfig = new MatSnackBarConfig();

    isLoading = false;

    constructor(
        private configService: ConfigurationService,
        private eventManager: JhiEventManager,
        private router: Router,
        private _migrationProcessService: MigrationProcessService,
        private _errorSnackBar: MatSnackBar,
        private _translationService: TranslateService
    ) {
        // Init snack bar configuration
        this.snackBarConfig.duration = 5000;
        this.snackBarConfig.verticalPosition = 'top';
        this.snackBarConfig.horizontalPosition = 'center';
    }

    ngOnInit() {
        this.configService.init();
        this.loadActiveMigrations();
        this.migrationRefreshTimer = setInterval(() => this.loadActiveMigrations(), HomeComponent.migrationRefreshInterval);
    }

    ngOnDestroy() {
        clearInterval(this.migrationRefreshTimer);
    }

    loadActiveMigrations() {
        if (this.isLoading) {
            // If slow database
            return;
        }
        this.isLoading = true;
        this._migrationProcessService.findActiveMigrations().subscribe(
            (res: HttpResponse<IMigration[]>) => {
                const migrations = res.body;
                migrations.sort((a: Migration, b: Migration) => a.id - b.id);
                this.migrations = migrations;
                this.isLoading = false;
            },
            () => this.openSnackBar('error.http.504')
        );
    }

    startStepper() {
        this.router.navigate(['/migration-init']);
    }

    checkMigration() {
        this.router.navigate(['/migration-check']);
    }

    /**
     * Add class according to status
     * @param status
     */
    cssClass(status: StatusEnum) {
        if (status === StatusEnum.DONE || status === StatusEnum.DONE_WITH_WARNINGS) {
            return 'badge-success';
        }
        if (status === StatusEnum.FAILED) {
            return 'badge-danger';
        }
        if (status === StatusEnum.RUNNING) {
            return 'badge-primary';
        }
        if (status === StatusEnum.WAITING) {
            return 'cell-waiting';
        }
    }

    /**
     * Open snack bar to display error message
     * @param errorCode
     */
    openSnackBar(errorCode: string, error = true) {
        if (error) {
            this.snackBarConfig.panelClass = ['errorPanel'];
        } else {
            this.snackBarConfig.panelClass = ['warnPanel'];
        }
        this._errorSnackBar.open(this._translationService.instant(errorCode), null, this.snackBarConfig);
    }
}
