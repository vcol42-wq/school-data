package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.WebView;
import java.io.File;

public class PdfPrint {
    private static final String TAG = "PdfPrint";
    private final PrintAttributes printAttributes;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(File file);
        void onFailure(String error);
    }

    public PdfPrint(PrintAttributes printAttributes) {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        if (printAttributes != null && printAttributes.getMediaSize() != null) {
            builder.setMediaSize(printAttributes.getMediaSize());
        } else {
            builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
        }
        if (printAttributes != null && printAttributes.getResolution() != null) {
            builder.setResolution(printAttributes.getResolution());
        } else {
            builder.setResolution(new PrintAttributes.Resolution("pdf_res", "PDF Resolution", 600, 600));
        }
        if (printAttributes != null && printAttributes.getMinMargins() != null) {
            builder.setMinMargins(printAttributes.getMinMargins());
        } else {
            builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
        }
        this.printAttributes = builder.build();
    }

    public void print(final WebView keepAliveWebView, final PrintDocumentAdapter printAdapter, final File path, final String fileName, final Callback callback) {
        try {
            final CancellationSignal layoutSignal = new CancellationSignal();
            final PrintAttributes oldAttributes = new PrintAttributes.Builder().build();
            final Bundle metadata = new Bundle();

            printAdapter.onLayout(oldAttributes, printAttributes, layoutSignal, new PrintDocumentAdapter.LayoutResultCallback() {
                @Override
                public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                    final File file = new File(path, fileName);
                    ParcelFileDescriptor pfd = null;
                    try {
                        if (!path.exists()) {
                            path.mkdirs();
                        }
                        if (file.exists()) {
                            file.delete();
                        }
                        file.createNewFile();
                        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to create destination file", e);
                        notifyFailure(callback, "فشل إنشاء ملف التخزين: " + e.getMessage());
                        return;
                    }

                    final ParcelFileDescriptor finalPfd = pfd;
                    final CancellationSignal writeSignal = new CancellationSignal();
                    printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, finalPfd, writeSignal, new PrintDocumentAdapter.WriteResultCallback() {
                        @Override
                        public void onWriteFinished(PageRange[] pages) {
                            super.onWriteFinished(pages);
                            closePfd(finalPfd);
                            // Reference keepAliveWebView to ensure it's not GC'd prematurely
                            if (keepAliveWebView != null) {
                                keepAliveWebView.getTag();
                            }
                            notifySuccess(callback, file);
                        }

                        @Override
                        public void onWriteFailed(CharSequence error) {
                            super.onWriteFailed(error);
                            closePfd(finalPfd);
                            if (keepAliveWebView != null) {
                                keepAliveWebView.getTag();
                            }
                            notifyFailure(callback, error != null ? error.toString() : "فشل كتابة مستند PDF");
                        }

                        @Override
                        public void onWriteCancelled() {
                            super.onWriteCancelled();
                            closePfd(finalPfd);
                            if (keepAliveWebView != null) {
                                keepAliveWebView.getTag();
                            }
                            notifyFailure(callback, "تم إلغاء كتابة PDF");
                        }
                    });
                }

                @Override
                public void onLayoutFailed(CharSequence error) {
                    super.onLayoutFailed(error);
                    if (keepAliveWebView != null) {
                        keepAliveWebView.getTag();
                    }
                    notifyFailure(callback, error != null ? error.toString() : "فشل تخطيط صفحة PDF");
                }

                @Override
                public void onLayoutCancelled() {
                    super.onLayoutCancelled();
                    if (keepAliveWebView != null) {
                        keepAliveWebView.getTag();
                    }
                    notifyFailure(callback, "تم إلغاء تخطيط PDF");
                }
            }, metadata);
        } catch (Exception e) {
            Log.e(TAG, "Exception during printAdapter.onLayout", e);
            notifyFailure(callback, e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void closePfd(ParcelFileDescriptor pfd) {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing file descriptor", e);
            }
        }
    }

    private void notifySuccess(final Callback callback, final File file) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(file);
                }
            });
        }
    }

    private void notifyFailure(final Callback callback, final String error) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onFailure(error);
                }
            });
        }
    }
}
