import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'jhi-migration-init.component',
    templateUrl: 'migration-init.component.html'
})
export class MigrationInitComponent {
    gitlabEnabled = true;
    svnEnabled = true;
    historyEnabled = true;
    mappingEnabled = false;
    cleaningEnabled = true;

    constructor(private _router: Router) {}

    goHome() {
        this._router.navigate(['/']);
    }

    start() {
        this._router.navigate(['/migration-process'], {
            queryParams: {
                gitlabEnabled: this.gitlabEnabled,
                svnEnabled: this.svnEnabled,
                historyEnabled: this.historyEnabled,
                mappingEnabled: this.mappingEnabled,
                cleaningEnabled: this.cleaningEnabled
            }
        });
    }

    setCleaning(checked: boolean) {
        this.cleaningEnabled = checked;
    }
    setMapping(checked: boolean) {
        this.mappingEnabled = checked;
    }
}
