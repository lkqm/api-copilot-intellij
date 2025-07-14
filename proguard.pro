-keepattributes *Annotation*

# state
-keep @com.intellij.openapi.components.Service class * { *; }
-keep @com.intellij.openapi.components.State class * { *; }
-keep class io.apicopilot.document.Document { *; }
-keep class io.apicopilot.document.Document$* { *; }

# model or config
-keep class io.apicopilot.codegen.model.** { *; }
-keep class io.apicopilot.codegen.context.** { *; }

# plugin.xml
-keep class io.apicopilot.window.ApiView { *; }
-keep class io.apicopilot.window.ApiViewImpl { *; }
-keep class io.apicopilot.window.ApiViewToolWindowFactory { *; }
-keep class io.apicopilot.search.ApiSearchEverywhereContributorFactory { *; }
-keep class io.apicopilot.editor.completion.PathCompletionContributor { *; }
-keep class io.apicopilot.editor.completion.PathCompletionConfidence { *; }
-keep class io.apicopilot.editor.completion.PathCompletionCharFilter { *; }
-keep class io.apicopilot.editor.completion.PathCompletionTypedHandler { *; }
-keep class io.apicopilot.editor.reference.PathReferenceContributor { *; }
-keep class io.apicopilot.editor.documentation.ApiDocumentationProvider { *; }
-keep class io.apicopilot.search.GotoApiAction { *; }
-keep class io.apicopilot.window.action.AddAction { *; }
-keep class io.apicopilot.window.action.RemoveAction { *; }
-keep class io.apicopilot.window.action.RefreshAction { *; }
-keep class io.apicopilot.window.action.PreviewAction { *; }
-keep class io.apicopilot.window.action.LocateApiAction { *; }

