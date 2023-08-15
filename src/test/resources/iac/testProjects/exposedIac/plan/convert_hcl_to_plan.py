""" Converts TLDB requirement data to test files """

import os
import subprocess
import shutil

import typer

TESTDIRS = ["applicable", "nonapplicable"]
PLAN_BINFILE = "tfplan.bin"
TMPDIR = "tmp"

g_tf_initialized = os.path.isfile(os.path.join(TMPDIR, ".terraform.lock.hcl"))


def handle_hcl_file(hcl_filepath: str, plan_filepath: str):
    print(f'Handling "{os.path.basename(hcl_filepath)}"...')

    if os.path.isfile(plan_filepath):
        print(f'Skipped since "{os.path.basename(plan_filepath)}" already exists')
        return

    # Copy the HCL file
    shutil.copy(hcl_filepath, ".")

    # Initialize Terraform once (must be done with some HCL file present)
    global g_tf_initialized
    if not g_tf_initialized:
        print('Initializing Terraform...')
        subprocess.check_call(["terraform", "init"], stdout=subprocess.DEVNULL)
        g_tf_initialized = True

    # Create the plan binary
    try:
        subprocess.check_call(["terraform", "plan", "--out", PLAN_BINFILE], stdout=subprocess.DEVNULL)
    except Exception:
        print(f'Failed converting "{hcl_filepath}"')
        raise

    # Convert plan binary to JSON
    subprocess.check_call(f"terraform show -json {PLAN_BINFILE} | jq '.' > {plan_filepath}", shell=True)

    # Cleanup
    os.unlink(os.path.basename(hcl_filepath))
    os.unlink(PLAN_BINFILE)


def main(hcl_dirpath: str, plan_dirpath: str):
    # Create output dir & move to it
    if not os.path.isdir(TMPDIR):
        os.mkdir(TMPDIR)
    hcl_dirpath = os.path.realpath(hcl_dirpath)
    plan_dirpath = os.path.realpath(plan_dirpath)
    curdir = os.getcwd()
    os.chdir(TMPDIR)

    # Handle each plan file
    for outdir in TESTDIRS:
        hcl_test_dirpath = os.path.join(hcl_dirpath, outdir)
        for hcl_filename in os.listdir(hcl_test_dirpath):
            plan_filename = os.path.splitext(hcl_filename)[0] + ".json"
            plan_filepath = os.path.join(plan_dirpath, outdir, plan_filename)
            hcl_filepath = os.path.join(hcl_test_dirpath, hcl_filename)
            handle_hcl_file(hcl_filepath, plan_filepath)

    # Cleanups
    os.chdir(curdir)
    os.rmdir(TMPDIR)
    print("!!!DONE!!!")


if "__main__" == __name__:
    typer.run(main)