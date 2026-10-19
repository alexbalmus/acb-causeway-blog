package com.alexbalmus.acbblog.webapp.wicket;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.causeway.viewer.wicket.model.causeway.WicketApplicationInitializer;
import org.apache.causeway.viewer.wicket.ui.components.attributes.AttributePanel;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.springframework.stereotype.Service;

/** Reject corrupt image files before Causeway converts an optional upload to null. */
@Service
public class WicketImageValidation implements WicketApplicationInitializer {
    @Override public void init(WebApplication application) {
        application.getComponentInstantiationListeners().add(component -> {
            if (component instanceof FileUploadField) component.add(new ImageFieldBehavior());
        });
    }

    private static final class ImageFieldBehavior extends Behavior {
        private static final long serialVersionUID = 1L;
        private boolean configured;

        @Override public void onConfigure(Component component) {
            if (configured) return;
            // The component's parent is available at configure time, after construction.
            var field = (FileUploadField) component;
            var panel = field.findParent(AttributePanel.class);
            if (panel != null && panel.attributeModel().isElementTypeSubtypeOf(BufferedImage.class)) {
                field.add(new ReadableImage());
            }
            configured = true;
        }
    }

    private static final class ReadableImage implements IValidator<List<FileUpload>> {
        private static final long serialVersionUID = 1L;

        @Override public void validate(IValidatable<List<FileUpload>> validatable) {
            if (validatable.getValue() == null) return;
            for (var upload : validatable.getValue()) {
                try (var input = upload.getInputStream()) {
                    if (ImageIO.read(input) != null) continue;
                } catch (IOException | IllegalArgumentException ignored) {
                    // Surface a field validation error and keep the persisted picture intact.
                }
                validatable.error(new ValidationError().setMessage(
                        "Could not read uploaded image. Choose a supported image such as PNG or JPEG."));
                return;
            }
        }
    }
}
