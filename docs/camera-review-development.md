# Camera review development plan

Approved design: replace the alert dialog with a native, system-theme-aware floating photo card. Animate captured photo from viewfinder bounds to card image bounds over 350 ms, reverse on Retake. Back discards capture. Respect Android animator settings (no app setting added). No VScreen.

Implementation: PhotoReviewView owns image decoding/orientation, layout, transition and cleanup. CameraActivity owns capture file and result delivery. Keep capture file until Use photo hands ownership to MainActivity; Retake/Back deletes it. Preserve pending review through configuration recreation. Keep main branch and public workflows untouched.

Checks: structural regression before implementation; full existing tests; compile/sign using existing public release key with versionCode above installed build; verify package identity and certificate; install with -r, never uninstall. Live animation is user-tested because VScreen is prohibited.

Review risks: rotation during review, Back during animation, background/resume while reviewing, double capture callbacks, old camera session callbacks. Block input during transitions; release animation/bitmap on destruction; don't reopen camera while review is active.
