/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MappingUpdateComponent } from 'app/entities/mapping/mapping-update.component';
import { MappingService } from 'app/entities/mapping/mapping.service';
import { Mapping } from 'app/shared/model/mapping.model';

describe('Component Tests', () => {
    describe('Mapping Management Update Component', () => {
        let comp: MappingUpdateComponent;
        let fixture: ComponentFixture<MappingUpdateComponent>;
        let service: MappingService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MappingUpdateComponent]
            })
                .overrideTemplate(MappingUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MappingUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MappingService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new Mapping(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.mapping = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );

            it(
                'Should call create service on save for new entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new Mapping();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.mapping = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );
        });
    });
});
