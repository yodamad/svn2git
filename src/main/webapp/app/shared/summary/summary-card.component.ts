import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';
import { ConfigurationService } from 'app/shared/service/configuration-service';

@Component({
    selector: 'jhi-summary-card',
    templateUrl: 'summary-card.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryCardComponent implements OnInit {
    @Input() migration: IMigration;

    svnUrl: string;
    gitlabUrl: string;

    constructor(private _configurationService: ConfigurationService) {}

    ngOnInit() {
        this._configurationService.gitlab().subscribe(res => (this.gitlabUrl = res));
        this._configurationService.svn().subscribe(res => (this.svnUrl = res));
    }
}
