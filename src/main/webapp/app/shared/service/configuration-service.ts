import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SERVER_API_URL } from 'app/app.constants';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GITLAB_URL, SVN_URL } from 'app/shared/constants/config.constants';

/**
 * Retrieve some configuration elements from backend
 */
@Injectable({ providedIn: 'root' })
export class ConfigurationService {
    private resourceUrl = SERVER_API_URL + 'api/config/';
    private svnUrl = this.resourceUrl + 'svn';
    private svnCredsOptionUrl = this.svnUrl + '/credentials';
    private gitlabUrl = this.resourceUrl + 'gitlab';
    private gitlabCredsOptionUrl = this.gitlabUrl + '/credentials';
    private overrideUrl = this.resourceUrl + 'override/';
    private extensionsUrl = this.overrideUrl + 'extensions';
    private mappingsUrl = this.overrideUrl + 'mappings';
    private flagsUrl = this.resourceUrl + 'flags';

    constructor(private http: HttpClient) {}

    /**
     * @return configured svn url
     */
    svn(): Observable<string> {
        return this.http.get(`${this.svnUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * @return configured svn credentials option
     */
    svnCredsOption(): Observable<string> {
        return this.http.get(`${this.svnCredsOptionUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * @return configured gitlab url
     */
    gitlab(): Observable<string> {
        return this.http.get(`${this.gitlabUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * @return configured gitlab credentials option
     */
    gitlabCredsOption(): Observable<string> {
        return this.http.get(`${this.gitlabCredsOptionUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * @return configuration for static extensions
     */
    overrideStaticExtensions(): Observable<boolean> {
        return this.http.get(`${this.extensionsUrl}`, { responseType: 'text' }).pipe(map(res => JSON.parse(res)));
    }

    /**
     * @return configuration for static mappings
     */
    overrideStaticMappings(): Observable<boolean> {
        return this.http.get(`${this.mappingsUrl}`, { responseType: 'text' }).pipe(map(res => JSON.parse(res)));
    }

    flagProjectCleaning(): Observable<boolean> {
        return this.http.get(`${this.flagsUrl}/projectCleaningOption`, { responseType: 'text' }).pipe(map(res => JSON.parse(res)));
    }

    /**
     * Init static configuration
     */
    init() {
        this.gitlab().subscribe(res => localStorage.setItem(GITLAB_URL, res));
        this.svn().subscribe(res => localStorage.setItem(SVN_URL, res));
    }
}
