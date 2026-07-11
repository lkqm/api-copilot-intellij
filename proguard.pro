-keepattributes *Annotation*

# JDK classes not on ProGuard's classpath
-dontwarn java.net.http.**
-dontwarn javax.xml.transform.**

# state
-keep @com.intellij.openapi.components.Service class * { *; }
-keep @com.intellij.openapi.components.State class * { *; }
-keep class io.apix.document.Document { *; }
-keep class io.apix.document.Document$* { *; }

# model or config
-keep class io.apix.codegen.model.** { *; }
-keep class io.apix.codegen.context.** { *; }

# plugin.xml
-keep class io.apix.window.ApiView { *; }
-keep class io.apix.window.ApiViewImpl { *; }
-keep class io.apix.window.ApiViewToolWindowFactory { *; }
-keep class io.apix.search.ApiSearchEverywhereContributorFactory { *; }
-keep class io.apix.editor.completion.PathCompletionContributor { *; }
-keep class io.apix.editor.completion.PathCompletionConfidence { *; }
-keep class io.apix.editor.completion.PathCompletionCharFilter { *; }
-keep class io.apix.editor.completion.PathCompletionTypedHandler { *; }
-keep class io.apix.editor.reference.PathReferenceContributor { *; }
-keep class io.apix.editor.documentation.ApiDocumentationProvider { *; }
-keep class io.apix.search.GotoApiAction { *; }
-keep class io.apix.window.action.AddAction { *; }
-keep class io.apix.window.action.RemoveAction { *; }
-keep class io.apix.window.action.RefreshAction { *; }
-keep class io.apix.window.action.PreviewAction { *; }
-keep class io.apix.window.action.LocateApiAction { *; }
-keep class io.apix.startup.DocumentUpdateStartupActivity { *; }
