using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;

namespace GitSvnWrapper
{
    // NB: Do not use recent C# constructs: we don't know what language level the C# compiler
    // used by powershell supports...
    public sealed class GitSvnCloneWrapper
    {
        private string text = ""; // useful for debugging
        private string lastError = "";
        private int rc;

        public GitSvnCloneWrapper() : this(@"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe") { }
        public GitSvnCloneWrapper(string powershellExecutablePath)
        {
            PowershellExecutablePath = powershellExecutablePath;
        }

        public string LastOutput { get { return text; } }
        public string LastError { get { return lastError; } }
        public int ReturnCode { get { return rc; } } // The return code of the git execution
        private string PowershellExecutablePath { get; set; }

        public int Execute(string url, string username, string password, string certificateAcceptResponse, string options)
        {
            return Execute(url, username, password, certificateAcceptResponse, options, 60);
        }

        public int Execute(string url, string username, string password, string certificateAcceptResponse, string options, int timeoutInSeconds)
        {
            var ok = false;
            try
            {
                // Clear last execution
                text = "";
                lastError = "";
                rc = 0;

                const string certificateHint = "(R)eject, accept (t)emporarily or accept (p)ermanently?";
                var passwordHint = string.Format("Password for '{0}':", username);
                var marker = Guid.NewGuid().ToString();
                var endMarker = marker + "#";

                // Build a minimalist powershell wrapper around the git command
                // The marker will allow us to determine that the git command execution completed
                // and to retrieve its return code.
                // NB: do not use { or } in the commands: driver.SendLine does not like it...
                var commands = new string[]
                {
                    string.Format("$marker='{0}'", marker),
                    string.Format("git svn clone {0} --username {1} {2}", url, username, options),
                    "\"$marker#$LASTEXITCODE#\""
                };

                var command = string.Join(";", commands);

                var driver = new AwaitDriver(PowershellExecutablePath);
                try
                {
                    driver.SendLine(command);

                    var timeout = TimeSpan.FromSeconds(timeoutInSeconds);
                    var now = DateTime.Now;
                    var sentT = false;
                    var sentPassword = false;
                    while (true)
                    {
                        var read = driver.ReadAllOutput();
                        Thread.Sleep(10);
                        if (string.IsNullOrEmpty(read))
                            continue;

                        text = read;
                        now = DateTime.Now;

                        if (text.Contains(certificateHint) && !sentT)
                        {
                            driver.SendLine(certificateAcceptResponse);
                            sentT = true;
                            now = DateTime.Now;
                        }

                        if (text.Contains(passwordHint) && !sentPassword)
                        {
                            driver.SendLine(password);
                            sentPassword = true;
                            now = DateTime.Now;
                        }

                        var markerLocation = text.LastIndexOf(endMarker);
                        if (markerLocation >= 0)
                        {
                            // Seems like git completed. Let's retrieve its return code
                            var begin = markerLocation + endMarker.Length;
                            var length = 0;
                            while (text[begin + length] != '#')
                                length++;

                            var rcText = text.Substring(begin, length);
                            rc = int.Parse(rcText);
                            break;
                        }

                        if (DateTime.Now - now > timeout) throw new TimeoutException(string.Format(
                            "Nothing was read from the console during the last {0} seconds", timeout));
                    }
                }
                finally { driver.Close(); }

                ok = true; // If we're here, everything went smoothly!
            }
            catch (Exception ex)
            {
                lastError = ex.ToString();
            }

#if DEBUG
            // Do not dump on production, because this would leak the password...
            Console.WriteLine("RC         : " + ReturnCode);
            Console.WriteLine("LAST ERROR : " + LastError);
            Console.WriteLine("LAST OUTPUT:\r\n" + LastOutput);
#endif

            return ok ? 0 : -1;
        }
    }

    // The code below was adapted and simplified from PowerShell Await Module (https://github.com/LeeHolmes/await)
    internal sealed class AwaitDriver
    {
        // Handles from the driver process
        private readonly Process driverHost;
        private readonly IntPtr hInput;
        private readonly IntPtr hOutput;

        // State to remember information between Read() calls so
        // that we don't have to scan the full buffer each time.
        private int lastReadPosition = 0;
        private List<string> lastRawContent = new List<string>();

        public AwaitDriver(string powershellExecutablePath)
        {
            // Launch a new instance of PowerShell and steal its console input / output handles
            var startInfo = new ProcessStartInfo(powershellExecutablePath, "-NoProfile");
#if !DEBUG
            // Be sure to hide the window on production, otherwise the password may be leaked...
            startInfo.WindowStyle = ProcessWindowStyle.Hidden;
#endif
            driverHost = Process.Start(startInfo);

            Thread.Sleep(100);

            NativeMethods.FreeConsole();
            NativeMethods.AttachConsole(driverHost.Id);

            const uint STD_INPUT_HANDLE = 0xFFFFFFF6;
            const uint STD_OUTPUT_HANDLE = 0xFFFFFFF5;
            hInput = NativeMethods.GetStdHandle(STD_INPUT_HANDLE);
            hOutput = NativeMethods.GetStdHandle(STD_OUTPUT_HANDLE);
        }

        // Sends the specified input to the driver process. Does not include a newline at the end of the input.
        // Input can be basic characters, or can use the same set of metacharacters
        // that the SendKeys() API supports, such as {ESC}.
        // http://msdn.microsoft.com/en-us/library/system.windows.forms.sendkeys.send(v=vs.110).aspx
        // While the syntax is the same as the SendKeys() API, the approach is not
        // based on SendKeys() at all - to ensure that applications can be automated while
        // you use your keyboard and mouse for other things.
        public void Send(string text)
        {
            var inputs = new List<NativeMethods.INPUT_RECORD>();

            foreach (var inputElement in SendKeysParser.Parse(text))
                foreach (var mappedInput in SendKeysParser.MapInput(inputElement))
                    inputs.Add(mappedInput);

            var eventsWritten = 0u;
            NativeMethods.WriteConsoleInput(hInput, inputs.ToArray(), (uint)inputs.Count, out eventsWritten);
        }

        public void SendLine(string input) { Send(input + "{ENTER}"); }

        public void Close()
        {
            driverHost.Kill();

            // Let's create a new console of our own
            NativeMethods.FreeConsole(); // Detach from the previous console
            NativeMethods.AllocConsole();
        }

        public string ReadAllOutput()
        {
            // Check the current console screen buffer dimensions, cursor coordinates / etc.
            NativeMethods.CONSOLE_SCREEN_BUFFER_INFO csbi;
            NativeMethods.GetConsoleScreenBufferInfo(hOutput, out csbi);

            var output = new StringBuilder();

            // If the cursor has gone before where we last scanned, then the screen
            // has been cleared and we should reset our state.
            if (lastReadPosition > csbi.dwCursorPosition.Y + 1)
                ResetLastScanInfo();

            // Figure out where to start and stop scanning
            var startReadPosition = 0;
            var endReadPosition = csbi.dwSize.Y - 1;

            // Go through each line in the buffer
            for (var row = startReadPosition; row <= endReadPosition; row++)
            {
                int width = csbi.dwMaximumWindowSize.X;
                var lpCharacter = new StringBuilder(width - 1);

                // Read the current line from the buffer
                NativeMethods.COORD dwReadCoord;
                dwReadCoord.X = 0;
                dwReadCoord.Y = (short)row;

                uint lpNumberOfCharsRead = 0;
                NativeMethods.ReadConsoleOutputCharacter(hOutput, lpCharacter, (uint)width, dwReadCoord, out lpNumberOfCharsRead);

                lastRawContent.Add(lpCharacter.ToString());
                if (lastRawContent.Count > 10)
                    lastRawContent.RemoveAt(0);

                output.AppendLine(lpCharacter.ToString().Substring(0, width).TrimEnd());
            }

            // Update our state to remember where to start scanning the buffer next.
            lastReadPosition = endReadPosition + 1;

            // We got content - return it.
            return output.ToString().TrimEnd();
        }

        private void ResetLastScanInfo()
        {
            lastReadPosition = 0;
            lastRawContent.Clear();
        }
    }

    internal static class SendKeysParser
    {
        private const ushort MAPVK_VK_TO_VSC = 0x00;

        public static List<string> Parse(string input)
        {
            var output = new List<string>();
            var scanningKeyName = false;
            var keyNameBuffer = new StringBuilder();

            // Iterate through the string
            for (var index = 0; index < input.Length; index++)
            {
                // Save the current item
                var currentChar = input[index];

                // We may have the start of a command
                if (currentChar == '{')
                {
                    if (scanningKeyName) throw new Exception(
                        "The character '{' is not a valid in a key name. To include the '{' character in your text, escape it with another: { {.");

                    // If it's escaped, then add it to output.
                    if (index < input.Length - 1 && input[index + 1] == '{')
                    {
                        output.Add(currentChar.ToString());
                        index++;
                    }
                    else // Otherwise, we found the start of a key name.
                        scanningKeyName = true;
                }
                else if (currentChar == '}')
                {
                    // We may have the end of a key name

                    // If it's escaped, then add it to output.
                    if (index < input.Length - 1 && input[index + 1] == '}')
                    {
                        // But not if we're scanning a key name
                        if (scanningKeyName) throw new Exception(
                            "The character '}' is not a valid in a key name. To include the '}' character in your text, escape it with another: } }.");

                        output.Add(currentChar.ToString());
                        index++;
                    }
                    else
                    {
                        // Not escaped

                        // If we're scanning a key name, record it.
                        if (scanningKeyName)
                        {
                            var keyName = keyNameBuffer.ToString();
                            if (string.IsNullOrEmpty(keyName))
                                throw new Exception("Key names may not be empty.");

                            output.Add(keyNameBuffer.ToString());
                            scanningKeyName = false;
                        }
                        else throw new Exception(
                            "The character '}' is not a valid by itself. To include the '}' character in your text, escape it with another: }}.");
                    }
                }
                else
                {
                    // Just a letter
                    if (scanningKeyName)
                        keyNameBuffer.Append(currentChar);
                    else
                        output.Add(currentChar.ToString());
                }
            }

            // We got to the end of the string.
            if (scanningKeyName) throw new Exception(
                "The character '{' (representing the start of a key name) did not have a matching '}' " +
                "character. To include the '{' character in your text, escape it with another: { {.");

            return output;
        }

        internal static List<NativeMethods.INPUT_RECORD> MapInput(string inputElement)
        {
            var inputs = new List<NativeMethods.INPUT_RECORD>();
            var input = new NativeMethods.INPUT_RECORD();
            input.EventType = 0x0001;

            var keypress = new NativeMethods.KEY_EVENT_RECORD();
            keypress.dwControlKeyState = 0;
            keypress.wRepeatCount = 1;

            // Just a regular character
            if (inputElement.Length == 1)
                keypress.UnicodeChar = inputElement[0];
            else
            {
                switch (inputElement.ToUpperInvariant())
                {
                    case "BACKSPACE":
                    case "BS":
                    case "BKSP":
                        keypress = GetKeyPressForSimpleKey(keypress, 0x08);
                        break;

                    case "BREAK":
                        keypress = GetKeyPressForSimpleKey(keypress, 0x03);
                        keypress.dwControlKeyState = (uint)NativeMethods.ControlKeyStates.LEFT_CTRL_PRESSED;
                        keypress.UnicodeChar = (char)0;
                        break;

                    case "ENTER":
                        keypress = GetKeyPressForSimpleKey(keypress, 0x0D);
                        break;

                    case "ESC":
                        keypress = GetKeyPressForSimpleKey(keypress, 0x1B);
                        break;
                }
            }

            keypress.bKeyDown = true;
            input.KeyEvent = keypress;
            inputs.Add(input);

            keypress.bKeyDown = false;
            keypress.dwControlKeyState = 0;
            input.KeyEvent = keypress;
            inputs.Add(input);

            return inputs;
        }

        private static NativeMethods.KEY_EVENT_RECORD GetKeyPressForSimpleKey(NativeMethods.KEY_EVENT_RECORD keypress, uint uCode)
        {
            keypress.UnicodeChar = (char)uCode;
            keypress.wVirtualKeyCode = (ushort)uCode;
            keypress.wVirtualScanCode = (ushort)NativeMethods.MapVirtualKey(uCode, MAPVK_VK_TO_VSC);
            return keypress;
        }
    }

    internal static class NativeMethods
    {
        [StructLayout(LayoutKind.Sequential)]
        internal struct COORD
        {
            internal short X;
            internal short Y;
        }

        internal struct SMALL_RECT
        {
            internal short Left;
            internal short Top;
            internal short Right;
            internal short Bottom;
        }

        internal struct CONSOLE_SCREEN_BUFFER_INFO
        {
            internal COORD dwSize;
            internal COORD dwCursorPosition;
            internal short wAttributes;
            internal SMALL_RECT srWindow;
            internal COORD dwMaximumWindowSize;
        }

        [StructLayout(LayoutKind.Explicit)]
        internal struct INPUT_RECORD
        {
            [FieldOffset(0)]
            internal ushort EventType;
            [FieldOffset(4)]
            internal KEY_EVENT_RECORD KeyEvent;
        };

        [StructLayout(LayoutKind.Explicit, CharSet = CharSet.Unicode)]
        internal struct KEY_EVENT_RECORD
        {
            [FieldOffset(0), MarshalAs(UnmanagedType.Bool)]
            internal bool bKeyDown;
            [FieldOffset(4), MarshalAs(UnmanagedType.U2)]
            internal ushort wRepeatCount;
            [FieldOffset(6), MarshalAs(UnmanagedType.U2)]
            internal ushort wVirtualKeyCode;
            [FieldOffset(8), MarshalAs(UnmanagedType.U2)]
            internal ushort wVirtualScanCode;
            [FieldOffset(10)]
            internal char UnicodeChar;
            [FieldOffset(12), MarshalAs(UnmanagedType.U4)]
            internal uint dwControlKeyState;
        }

        internal enum ControlKeyStates
        {
            RIGHT_ALT_PRESSED = 0x1,
            LEFT_ALT_PRESSED = 0x2,
            RIGHT_CTRL_PRESSED = 0x4,
            LEFT_CTRL_PRESSED = 0x8,
            SHIFT_PRESSED = 0x10,
            NUMLOCK_ON = 0x20,
            SCROLLLOCK_ON = 0x40,
            CAPSLOCK_ON = 0x80,
            ENHANCED_KEY = 0x100
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        internal static extern bool AttachConsole(int pid);

        [DllImport("kernel32.dll")]
        internal static extern bool FreeConsole();

        [DllImport("kernel32.dll")]
        internal static extern bool AllocConsole();

        [DllImport("kernel32.dll", SetLastError = true)]
        internal static extern bool WriteConsoleInput(IntPtr hConsoleInput, INPUT_RECORD[] lpBuffer, uint nLength, out uint lpNumberOfEventsWritten);

        [DllImport("user32.dll")]
        internal static extern uint MapVirtualKey(uint uCode, uint uMapType);

        [DllImport("kernel32.dll")]
        internal static extern IntPtr GetStdHandle(uint nStdHandle);

        [DllImport("kernel32.dll", SetLastError = true)]
        internal static extern bool FlushConsoleInputBuffer(IntPtr hConsoleInput);

        [DllImport("Kernel32")]
        internal static extern bool ReadConsoleOutputCharacter(IntPtr hConsoleOutput, StringBuilder lpCharacter, uint nLength, COORD dwReadCoord, out uint lpNumberOfCharsRead);

        [DllImport("kernel32.dll")]
        internal static extern bool GetConsoleScreenBufferInfo(IntPtr hConsoleOutput, out CONSOLE_SCREEN_BUFFER_INFO lpConsoleScreenBufferInfo);
    }
}
