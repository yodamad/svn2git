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
    private gitlabUrl = this.resourceUrl + 'gitlab';

    constructor(private http: HttpClient) {}

    /**
     * @return configured svn url
     */
    svn(): Observable<string> {
        return this.http.get(`${this.svnUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * @return configured gitlab url
     */
    gitlab(): Observable<string> {
        return this.http.get(`${this.gitlabUrl}`, { responseType: 'text' }).pipe(map(res => res));
    }

    /**
     * Init static configuration
     */
    init() {
        this.gitlab().subscribe(res => localStorage.setItem(GITLAB_URL, res));
        this.svn().subscribe(res => localStorage.setItem(SVN_URL, res));
    }
}
