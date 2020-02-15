import { Component, OnInit } from '@angular/core';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { LoginModalService, Principal, Account } from 'app/core';
import { Router } from '@angular/router';
import { ConfigurationService } from 'app/shared/service/configuration-service';
import { IMigration, StatusEnum } from 'app/shared/model/migration.model';
import { HttpResponse } from '@angular/common/http';
import { MigrationProcessService } from 'app/migration/migration-process.service';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-home',
    templateUrl: './home.component.html',
    styleUrls: ['home.scss']
})
export class HomeComponent implements OnInit {
    account: Account;
    modalRef: NgbModalRef;

    // migrations in Running or Waiting State initialised in ngOnInit
    migrations: IMigration[] = [];

    // last migrations
    lastMigrations: IMigration[] = [];

    // SnackBar config
    snackBarConfig = new MatSnackBarConfig();

    constructor(
        private principal: Principal,
        private loginModalService: LoginModalService,
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
        this.principal.identity().then(account => {
            this.account = account;
        });
        this.registerAuthenticationSuccess();
        this.configService.init();

        this._migrationProcessService
            .findActiveMigrations()
            .subscribe((res: HttpResponse<IMigration[]>) => (this.migrations = res.body), () => this.openSnackBar('error.http.504'));

        this._migrationProcessService
            .findLastMigrations(5)
            .subscribe(res => (this.lastMigrations = res.body), () => this.openSnackBar('error.http.504'));
    }

    registerAuthenticationSuccess() {
        this.eventManager.subscribe('authenticationSuccess', message => {
            this.principal.identity().then(account => {
                this.account = account;
            });
        });
    }

    isAuthenticated() {
        return this.principal.isAuthenticated();
    }

    login() {
        this.modalRef = this.loginModalService.open();
    }

    startStepper() {
        this.router.navigate(['/migration-process']);
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
