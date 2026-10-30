# Contributing to AegisGuard Android

Thank you for your interest in contributing to AegisGuard!

## Verification
Before creating a pull request, run the resource integrity verification:
```bash
python tools/verify_ids.py
```
Ensure all DNS packet parsing and VpnService logic maintain zero-root compatibility.
