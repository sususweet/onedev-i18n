package io.onedev.server.web.page.project.setting.general;

import static io.onedev.server.model.Project.PROP_CODE_MANAGEMENT_ENABLED;
import static io.onedev.server.model.Project.PROP_DESCRIPTION;
import static io.onedev.server.model.Project.PROP_ISSUE_MANAGEMENT_ENABLED;
import static io.onedev.server.model.Project.PROP_NAME;

import java.io.Serializable;
import java.util.Collection;

import org.apache.shiro.authz.UnauthorizedException;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.collect.Sets;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.Project;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.Path;
import io.onedev.server.util.PathNode;
import io.onedev.server.web.WebSession;
import io.onedev.server.web.component.project.ConfirmDeleteProjectModal;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.page.project.ProjectListPage;
import io.onedev.server.web.page.project.setting.ProjectSettingPage;

@SuppressWarnings("serial")
public class GeneralProjectSettingPage extends ProjectSettingPage {

	private String oldPath;
	
	private BeanEditor editor;
	
	public GeneralProjectSettingPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Collection<String> properties = Sets.newHashSet(PROP_NAME, PROP_DESCRIPTION, 
				PROP_CODE_MANAGEMENT_ENABLED, PROP_ISSUE_MANAGEMENT_ENABLED);
		
		DefaultRoleBean defaultRoleBean = new DefaultRoleBean();
		defaultRoleBean.setRole(getProject().getDefaultRole());
		
		ParentBean parentBean = new ParentBean();
		parentBean.setParent(getProject().getParent());
		
		editor = BeanContext.editModel("editor", new IModel<Serializable>() {

			@Override
			public void detach() {
			}

			@Override
			public Serializable getObject() {
				return getProject();
			}

			@Override
			public void setObject(Serializable object) {
				// check contract of projectManager.save on why we assign oldPath here
				oldPath = getProject().getPath();
				editor.getDescriptor().copyProperties(object, getProject());
			}
			
		}, properties, false);
		
		BeanEditor defaultRoleEditor = BeanContext.edit("defaultRoleEditor", defaultRoleBean);		
		BeanEditor parentEditor = BeanContext.edit("parentEditor", parentBean);
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onError() {
				super.onError();
			}

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				Project parent = parentBean.getParent();
				if (parent != null && !SecurityUtils.canCreateChildren(parent) 
						|| parent == null && !SecurityUtils.canCreateRootProjects()) {
					throw new UnauthorizedException();
				}
				
				Project project = getProject();
				if (parent != null && project.isSelfOrAncestorOf(parent)) {
					parentEditor.error(new Path(new PathNode.Named("parentPath")), 
							"Can not use current or descendant project as parent");
				} else {
					project.setParent(parent);
					ProjectManager projectManager = OneDev.getInstance(ProjectManager.class);
					Project projectWithSameName = projectManager.find(parent, project.getName());
					if (projectWithSameName != null && !projectWithSameName.equals(project)) {
						if (parent != null) {
							editor.error(new Path(new PathNode.Named("name")),
									"This name has already been used by another child project");
						} else {
							editor.error(new Path(new PathNode.Named("name")),
									"This name has already been used by another root project");
						}
					} else {
						project.setDefaultRole(defaultRoleBean.getRole());
						projectManager.save(project, oldPath);
						Session.get().success("General setting has been updated");
						setResponsePage(GeneralProjectSettingPage.class, paramsOf(project));
					}
				}
			}
			
		};
		form.add(editor);
		form.add(defaultRoleEditor);
		form.add(parentEditor);
		
		form.add(new AjaxLink<Void>("delete") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				new ConfirmDeleteProjectModal(target) {
					
					@Override
					protected void onDeleted(AjaxRequestTarget target) {
						String redirectUrlAfterDelete = WebSession.get().getRedirectUrlAfterDelete(Project.class);
						if (redirectUrlAfterDelete != null)
							throw new RedirectToUrlException(redirectUrlAfterDelete);
						else
							setResponsePage(ProjectListPage.class);
					}
					
					@Override
					protected Project getProject() {
						return GeneralProjectSettingPage.this.getProject();
					}

				};
			}
			
		});
		
		add(form);
	}

	@Override
	protected Component newProjectTitle(String componentId) {
		return new Label(componentId, "<span class='text-truncate'>General Setting</span>").setEscapeModelStrings(false);
	}

}
