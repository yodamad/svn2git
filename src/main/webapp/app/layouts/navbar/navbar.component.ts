import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { VERSION } from 'app/app.constants';
import { JhiLanguageHelper } from 'app/core';
import { ProfileService } from '../profiles/profile.service';

@Component({
    selector: 'jhi-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['navbar.scss']
})
export class NavbarComponent implements OnInit {
    inProduction: boolean;
    isNavbarCollapsed: boolean;
    languages: any[];
    swaggerEnabled: boolean;
    version: string;

    constructor(
        private languageService: JhiLanguageService,
        private languageHelper: JhiLanguageHelper,
        private profileService: ProfileService,
        private router: Router
    ) {
        // TODO : Deploy Prod from a release and not a snapshot
        this.version = VERSION ? 'v' + VERSION.replace('-SNAPSHOT', '') : '';
        this.isNavbarCollapsed = true;
    }

    ngOnInit() {
        this.languageHelper.getAll().then(languages => {
            this.languages = languages;
        });

        this.profileService.getProfileInfo().then(profileInfo => {
            this.inProduction = profileInfo.inProduction;
            this.swaggerEnabled = profileInfo.swaggerEnabled;
        });
    }

    changeLanguage(languageKey: string) {
        this.languageService.changeLanguage(languageKey);
    }

    collapseNavbar() {
        this.isNavbarCollapsed = true;
    }

    logout() {
        this.collapseNavbar();
        this.router.navigate(['']);
    }

    toggleNavbar() {
        this.isNavbarCollapsed = !this.isNavbarCollapsed;
    }
}
