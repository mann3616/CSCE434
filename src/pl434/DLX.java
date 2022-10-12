package pl434;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;

public class DLX {

  private DLX() {
    throw new IllegalStateException("Utility class");
  }

  // Processor Emulation ========================================================
  private static int[] R = new int[32];
  private static int PC, op, a, b, c, format;

  private static final int MEM_SIZE = 10000;
  private static int[] M = new int[MEM_SIZE / 4];

  public static void load(int[] program) {
    M = Arrays.copyOf(program, M.length);
    M[program.length] = -1;
  }

  // epsilon for precision of float comparisons
  public static final float EPS = 0.01f;

  // for processing input in execute
  private static int currentLine = -1;
  private static StringTokenizer st = null;

  public static void execute(InputStream in) throws IOException {
    int origC = 0; // used for F2 instruction RET
    float fC = 0f; // used for F1/F2 instructions fOP
    for (int i = 0; i < 32; i++) {
      R[i] = 0;
    }
    R[30] = MEM_SIZE - 1;
    PC = 0;

    String line;
    try (
      BufferedReader reader = new BufferedReader(new InputStreamReader(in))
    ) {
      boolean returned = false;
      while (!returned) {
        // // uncomment to iteratively step through program
        // System.err.print(instrString(M[PC]));
        // for (int i = 31; i > 27; i--) {
        // 	System.err.println("R[" + i + "] :: " + R[i]);
        // }
        // for (int i = 8; i > 0; i--) {
        // 	System.err.println("R[" + i + "] :: " + R[i]);
        // }
        // for (int i = 0; i < 40; i += 4) {
        // 	System.err.println("--M[" + (R[30]-i)/4 + "] :: " + M[(R[30]-i)/4]);
        // }
        // try {
        //     System.in.read();
        // }
        // catch (Exception e) {
        //     e.printStackTrace();
        // }

        R[0] = 0;
        disassemble(M[PC]); // init op, a, b, c

        int nextPC = PC + 1;
        if (format == 1) {
          fC = toFP32FromFP16(c);
        }
        if (format == 2) {
          origC = c;
          c = R[origC];
          fC = fR(origC);
        }
        switch (op) {
          case ADD:
          case ADDI:
            R[a] = R[b] + c;
            break;
          case fADD:
          case fADDI:
            fR(a, fR(b) + fC);
            break;
          case SUB:
          case SUBI:
            R[a] = R[b] - c;
            break;
          case fSUB:
          case fSUBI:
            fR(a, fR(b) - fC);
            break;
          case MUL:
          case MULI:
            R[a] = R[b] * c;
            break;
          case fMUL:
          case fMULI:
            fR(a, fR(b) * fC);
            break;
          case DIV:
          case DIVI:
            R[a] = R[b] / c;
            break;
          case fDIV:
          case fDIVI:
            fR(a, fR(b) / fC);
            break;
          case MOD:
          case MODI:
            R[a] = R[b] % c;
            break;
          case fMOD:
          case fMODI:
            fR(a, fR(b) % fC);
            break;
          case POW:
          case POWI:
            if (R[b] < 0 || c < 0) {
              System.err.println(
                "DLX.execute: Illegal value (" + R[b] + ")^(" + c + ") in POW!"
              );
              bug(1);
            }
            R[a] = (int) Math.round(Math.pow(R[b], c));
            break;
          case CMP:
          case CMPI:
            R[a] = R[b] - c;
            if (R[a] < 0) {
              R[a] = -1;
            } else if (R[a] > 1) {
              R[a] = 1;
            }
            break;
          case fCMP:
          case fCMPI:
            float result = fR(b) - fC;

            R[a] = 0;
            if (result <= -EPS) {
              R[a] = -1;
            } else if (result >= EPS) {
              R[a] = 1;
            }
            break;
          case OR:
          case ORI:
            R[a] = R[b] | c;
            break;
          case AND:
          case ANDI:
            R[a] = R[b] & c;
            break;
          case BIC:
          case BICI:
            R[a] = R[b] & ~c;
            break;
          case XOR:
          case XORI:
            R[a] = R[b] ^ c;
            break;
          case LSH:
          case LSHI:
            if (c < -31 || c > 31) {
              System.err.println(
                "DLX.execute: Illegal value " + c + " in LSH!"
              );
              bug(1);
            }

            if (c < 0) {
              R[a] = R[b] >>> -c;
            } else {
              R[a] = R[b] << c;
            }
            break;
          case ASH:
          case ASHI:
            if (c < -31 || c > 31) {
              System.err.println(
                "DLX.execute: Illegal value " + c + " in ASH!"
              );
              bug(1);
            }

            if (c < 0) {
              R[a] = R[b] >> -c;
            } else {
              R[a] = R[b] << c;
            }
            break;
          case CHK:
          case CHKI:
            if (R[a] < 0) {
              System.err.println(
                "DLX.execute: " +
                (4 * PC) +
                " :: R[" +
                a +
                "] == " +
                R[a] +
                " < 0"
              );
              bug(39);
            } else if (R[a] >= c) {
              System.err.println(
                "DLX.execute: " +
                (4 * PC) +
                " :: R[" +
                a +
                "] == " +
                R[a] +
                " >= " +
                c
              );
              bug(39);
            }
            break;
          case LDW:
          case LDX:
            R[a] = M[(R[b] + c) / 4];
            break;
          case POP:
            R[a] = M[R[b] / 4];
            R[b] = R[b] + c;
            break;
          case STW:
          case STX:
            M[(R[b] + c) / 4] = R[a];
            break;
          case PSH:
            R[b] = R[b] + c;
            M[R[b] / 4] = R[a];
            break;
          case ARRCPY:
            for (int i = 0; i < c; i++) {
              M[(R[a] - 4 * i) / 4] = M[(R[b] - 4 * i) / 4];
            }
            break;
          case BEQ:
            if (R[a] == 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(47);
            }
            break;
          case BNE:
            if (R[a] != 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(48);
            }
            break;
          case BLT:
            if (R[a] < 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(49);
            }
            break;
          case BGE:
            if (R[a] >= 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(50);
            }
            break;
          case BLE:
            if (R[a] <= 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(51);
            }
            break;
          case BGT:
            if (R[a] > 0) {
              nextPC = PC + c;
            }
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(52);
            }
            break;
          case BSR:
            R[31] = 4 * (PC + 1);
            nextPC = PC + c;
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(53);
            }
            break;
          case JSR:
            R[31] = 4 * (PC + 1);
            nextPC = c / 4;
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(54);
            }
            break;
          case RET:
            if (origC == 0) {
              returned = true;
              break;
            }
            nextPC = c / 4;
            if (nextPC < 0 || nextPC > MEM_SIZE / 4) {
              System.err.println(
                "DLX.execute: " +
                (4 * nextPC) +
                " is no address " +
                "in memory [0, " +
                MEM_SIZE +
                "]."
              );
              bug(55);
            }
            break;
          case RDI:
            System.out.print("int? ");
            line = nextInput(reader);
            R[a] = Integer.parseInt(line);
            break;
          case RDF:
            System.out.print("float? ");
            line = nextInput(reader);
            fR(a, Float.parseFloat(line));
            break;
          case RDB:
            System.out.print("true or false? ");
            line = nextInput(reader);
            R[a] = (Boolean.parseBoolean(line) ? 1 : 0);
            break;
          case WRI:
            System.out.print(R[b] + " ");
            break;
          case WRF:
            System.out.printf("%.2f ", fR(b));
            break;
          case WRB:
            System.out.print((R[b] == 1) + " ");
            break;
          case WRL:
            System.out.println();
            break;
          case ERR:
            System.err.println("Program dropped off the end!");
            break;
          default:
            System.err.println("DLX.execute: Unknown opcode encountered!");
            bug(2);
            break;
        }
        PC = nextPC;
      }
    } catch (NumberFormatException e) {
      throw new NumberFormatException(
        "Failed at line " + currentLine + " of input: " + e.getMessage()
      );
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("Failed at " + (4 * PC));
      System.err.println("Instruction :: " + instrString(M[PC]));
      bug(63);
    }
  }

  // form input strings from line of input
  private static String nextInput(BufferedReader reader) throws IOException {
    while (st == null || !st.hasMoreElements()) {
      try {
        st = new StringTokenizer(reader.readLine());
        currentLine++;
      } catch (IOException e) {
        System.out.println("error");
        System.err.println("Interepter: Couldn't read data file");
        throw e;
      }
    }
    return st.nextToken();
  }

  // put val in R[idx]
  private static void fR(int idx, float val) {
    R[idx] = Float.floatToIntBits(val);
  }

  // get float value from R[idx]
  private static float fR(int idx) {
    return Float.intBitsToFloat(R[idx]);
  }

  // Half-Precision Floating-Point (FP16) Support ===============================

  /*
   * Functionality inspired by and built off the following reference sources:
   * 		- https://en.wikipedia.org/wiki/Half-precision_floating-point_format
   * 		- http://www.fox-toolkit.org/ftp/fasthalffloatconversion.pdf
   * 		- https://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java
   * 		- https://stackoverflow.com/questions/1659440/32-bit-to-16-bit-floating-point-conversion
   * 		- https://stackoverflow.com/questions/5678432/decompressing-half-precision-floats-in-javascript
   *
   * Tools used during development:
   * 		- http://evanw.github.io/float-toy/
   * 		- https://www.rapidtables.com/convert/number/hex-to-binary.html
   *
   * All magic numbers not declared static final arise from the following:
   * 		FP16 spec - 1-bit sign, 5-bit exp w/bias  15, 10-bit mant
   * 		FP32 spec - 1-bit sign, 8-bit exp w/bias 127, 23-bit mant
   *
   * 		FP16 representation - s	eee ee		mm mmmm mmmm
   * 		FP32 representation - s	eee eeee e	mmm mmmm mmmm mmmm mmmm mmmm
   *
   * 		In general, FP16 exp is 5 MSBs of FP32 exp and FP16 mant is 10 MSBs of FP32 mant
   */

  // FP32 boundaries for converion to FP16
  private static final int FP32_SUB = 0x33000000;
  private static final int FP32_MIN = 0x38800000;
  private static final int FP32_MAX = 0x47800000;
  private static final int FP32_INF = 0x7f800000;

  // FP16 infinity for conversion to FP32
  private static final int FP16_INF = 0x7c00;

  // floating-point exponent bias
  private static final int FP32_BIAS = 127;
  private static final int FP16_BIAS = 15;

  // convert half-precision floating-point to half-precision floating-point
  private static float toFP32FromFP16(int hbits) {
    int sign = (hbits & 0x8000) << 16; // bitmask to collect sign for FP32
    int exp = (hbits & 0x7c00); // bitmask to collect FP16 exp w/o sign or mant
    int mant = hbits & 0x03ff; // bitmask to collect FP16 mant w/o sign or exp

    if (exp == FP16_INF) { // FP16_INF to FP32_INF
      exp = FP32_INF >>> 13; // bitshift because later it's shifted back
    } else if (exp != 0) { // normal FP16
      exp = ((exp >> 10) - FP16_BIAS + FP32_BIAS) << 10;
    } else if (mant != 0) { // subnormal FP16
      exp = FP32_MIN >>> 13; // normal FP32 exp
      int sub = 0x01 << 10; // subnormal FP16 bit
      do {
        mant <<= 1; // multiple mant by 2
        exp -= sub; // subtract 1 from exp
      } while ((mant & sub) == 0); // until no leading zero
      mant &= 0x03FF; // bitmask to ignore subnormal bit
    }

    // building FP32
    int val = exp | mant; // combine exp and mant
    val <<= 13; // bitshift to align bits with FP32 spec
    return Float.intBitsToFloat(sign | val);
  }

  // convert single-precision floating-point to half-precision floating-point
  private static int fromFP32ToFP16(float fval) {
    int bits = Float.floatToIntBits(fval); // convert FP32 to bitstring
    int sign = (bits >>> 16) & 0x8000; // bitmask to collect sign for FP16
    int val = bits & ~(sign << 16); // bitmask to collect FP32 exp and mant w/o sign
    int mant = (val >>> 13) & 0x03FF; // bitmask to collect FP16 mant w/o exp or sign

    if (val >= FP32_MAX) { // value too large to store in FP16, convert to +/- INF
      if (val < FP32_INF) { // value was not previously +/- INF
        return sign | FP16_INF;
      }
      // value was previously +/- INF (or NaN), incl mant bits
      return sign | FP16_INF | mant;
    }

    val += 0x1000; // round up for conversion from FP32 to FP16
    if (val >= FP32_MAX) { // value too large as result of rounding, return FP16 max
      return sign | (FP16_INF - 0x01);
    }

    int exp = (val >>> 23) & 0xFF; // bitmask to collect FP16 exp w/o sign
    mant = (val >>> 13) & 0x03FF; // bitmask to collect FP16 mant w/o exp or sign
    if (val >= FP32_MIN) { // normalized FP16
      return sign | (exp - FP32_BIAS + FP16_BIAS) << 10 | mant;
    }
    if (val < FP32_SUB) { // value too small to represent in FP16, return +/- 0
      return sign;
    }

    // denormalized FP16
    exp = (bits >>> 23) & 0xFF; // bitmask to collect FP32 exp w/o sign; removes rounding
    mant = bits & 0x007FFFFF; // bitmask to collect FP32 mant w/o exp or sign
    int sub = 0x01 << 23; // subnormal FP32 bit

    val = mant | sub; // add subnormal bit to mant
    val >>>= (13 + FP32_BIAS - FP16_BIAS - exp); // bitshift to align bits with FP16 spec
    val += 0x01; // round up for conversion from FP32 to FP16
    val >>>= 1; // bitshift to account for subnormal conversion
    return sign | val;
  }

  // Opcode Handling ============================================================
  private static final String[] mnemo = {
    "ADD",
    "SUB",
    "MUL",
    "DIV",
    "MOD",
    "POW",
    "CMP",
    "fADD",
    "fSUB",
    "fMUL",
    "fDIV",
    "fMOD",
    "fCMP",
    "OR",
    "AND",
    "BIC",
    "XOR",
    "LSH",
    "ASH",
    "CHK",
    "ADDI",
    "SUBI",
    "MULI",
    "DIVI",
    "MODI",
    "POWI",
    "CMPI",
    "fADDI",
    "fSUBI",
    "fMULI",
    "fDIVI",
    "fMODI",
    "fCMPI",
    "ORI",
    "ANDI",
    "BICI",
    "XORI",
    "LSHI",
    "ASHI",
    "CHKI",
    "LDW",
    "LDX",
    "POP",
    "STW",
    "STX",
    "PSH",
    "ARRCPY",
    "BEQ",
    "BNE",
    "BLT",
    "BGE",
    "BLE",
    "BGT",
    "BSR",
    "JSR",
    "RET",
    "RDI",
    "RDF",
    "RDB",
    "WRI",
    "WRF",
    "WRB",
    "WRL",
    "ERR",
  };

  // arithmetic with F2 format
  static final int ADD = 0;
  static final int SUB = 1;
  static final int MUL = 2;
  static final int DIV = 3;
  static final int MOD = 4;
  static final int POW = 5;
  static final int CMP = 6;

  static final int fADD = 7;
  static final int fSUB = 8;
  static final int fMUL = 9;
  static final int fDIV = 10;
  static final int fMOD = 11;
  static final int fCMP = 12;

  static final int OR = 13;
  static final int AND = 14;
  static final int BIC = 15;
  static final int XOR = 16;
  static final int LSH = 17;
  static final int ASH = 18;

  static final int CHK = 19;

  // arithmetic with F1 format
  static final int ADDI = 20;
  static final int SUBI = 21;
  static final int MULI = 22;
  static final int DIVI = 23;
  static final int MODI = 24;
  static final int POWI = 25;
  static final int CMPI = 26;

  static final int fADDI = 27;
  static final int fSUBI = 28;
  static final int fMULI = 29;
  static final int fDIVI = 30;
  static final int fMODI = 31;
  static final int fCMPI = 32;

  static final int ORI = 33;
  static final int ANDI = 34;
  static final int BICI = 35;
  static final int XORI = 36;
  static final int LSHI = 37;
  static final int ASHI = 38;

  static final int CHKI = 39;

  // load/store
  static final int LDW = 40;
  static final int LDX = 41;
  static final int POP = 42;

  static final int STW = 43;
  static final int STX = 44;
  static final int PSH = 45;

  static final int ARRCPY = 46;

  // control
  static final int BEQ = 47;
  static final int BNE = 48;
  static final int BLT = 49;
  static final int BGE = 50;
  static final int BLE = 51;
  static final int BGT = 52;

  static final int BSR = 53;
  static final int JSR = 54;
  static final int RET = 55;

  // input/output
  static final int RDI = 56;
  static final int RDF = 57;
  static final int RDB = 58;
  static final int WRI = 59;
  static final int WRF = 60;
  static final int WRB = 61;
  static final int WRL = 62;

  // error
  static final int ERR = 63;

  private static void disassemble(int instrWord) {
    op = instrWord >>> 26;
    a = (instrWord >>> 21) & 0x1F;
    b = (instrWord >>> 16) & 0x1F;
    switch (op) {
      // F1 format
      case ADDI:
      case SUBI:
      case MULI:
      case DIVI:
      case MODI:
      case POWI:
      case CMPI:
      case fADDI:
      case fSUBI:
      case fMULI:
      case fDIVI:
      case fMODI:
      case fCMPI:
      case ORI:
      case ANDI:
      case BICI:
      case XORI:
      case LSHI:
      case ASHI:
      case CHKI:
      case LDW:
      case POP:
      case STW:
      case PSH:
      case BEQ:
      case BNE:
      case BLT:
      case BGE:
      case BLE:
      case BGT:
      case BSR:
      case WRL:
        format = 1;
        c = (short) instrWord;
        break;
      // F2 format
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case MOD:
      case POW:
      case CMP:
      case fADD:
      case fSUB:
      case fMUL:
      case fDIV:
      case fMOD:
      case fCMP:
      case OR:
      case AND:
      case BIC:
      case XOR:
      case LSH:
      case ASH:
      case CHK:
      case LDX:
      case STX:
      case ARRCPY:
      case RET:
      case RDI:
      case RDF:
      case RDB:
      case WRI:
      case WRF:
      case WRB:
        format = 2;
        c = instrWord & 0x1F;
        break;
      // F3 format
      case JSR:
        format = 3;
        a = -1;
        b = -1;
        c = instrWord & 0x3FFFFFF;
        break;
      // error or unknown
      case ERR:
      default:
        System.err.println("Illegal instruction: (" + PC + ")!");
        break;
    }
  }

  public static String instrString(int instrWord) {
    disassemble(instrWord);
    String line = mnemo[op];

    switch (op) {
      case WRL:
        return line + "\n";
      case BSR:
      case RET:
      case JSR:
        return line + " " + c + "\n";
      case RDB:
      case RDI:
      case RDF:
        return line + " " + a + "\n";
      case WRB:
      case WRI:
      case WRF:
        return line + " " + b + "\n";
      case CHKI:
      case BEQ:
      case BNE:
      case BLT:
      case BGE:
      case BLE:
      case BGT:
      case CHK:
        return line + " " + a + " " + c + "\n";
      case ADDI:
      case SUBI:
      case MULI:
      case DIVI:
      case MODI:
      case POWI:
      case CMPI:
      case ORI:
      case ANDI:
      case BICI:
      case XORI:
      case LSHI:
      case ASHI:
      case LDW:
      case POP:
      case STW:
      case PSH:
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case MOD:
      case POW:
      case CMP:
      case fADD:
      case fSUB:
      case fMUL:
      case fDIV:
      case fMOD:
      case fCMP:
      case OR:
      case AND:
      case BIC:
      case XOR:
      case LSH:
      case ASH:
      case LDX:
      case STX:
      case ARRCPY:
        return line + " " + a + " " + b + " " + c + "\n";
      case fADDI:
      case fSUBI:
      case fMULI:
      case fDIVI:
      case fMODI:
      case fCMPI:
        return (
          line +
          " " +
          a +
          " " +
          b +
          " " +
          String.format("%.2f", toFP32FromFP16(c)) +
          "\n"
        );
      case ERR:
      default:
        break;
    }
    return line;
  }

  private static void bug(int n) {
    if (R[30] != 0) {
      for (int i = 31; i > 27; i--) {
        System.err.println("R[" + i + "] :: " + R[i]);
      }
      for (int i = 8; i > 0; i--) {
        System.err.println("R[" + i + "] :: " + R[i]);
      }
      for (int i = 0; i < 40; i += 4) {
        System.err.println(
          "--M[" + (R[30] - i) / 4 + "] :: " + M[(R[30] - i) / 4]
        );
      }
    }

    try {
      System.in.read();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(n);
  }

  private static int F1(int op, int a, int b, int c) {
    if (c < 0) {
      c ^= 0xFFFF0000;
    }
    if ((a & ~0x1F | b & ~0x1F | c & ~0xFFFF) != 0) {
      System.err.println("Illegal Operand(s) for F1 format: " + mnemo[op]);
      bug(1);
    }
    return (op << 26 | a << 21 | b << 16 | c);
  }

  private static int F1(int op, int a, int b, float c) {
    int half = fromFP32ToFP16(c);
    if ((a & ~0x1F | b & ~0x1F | half & ~0xFFFF) != 0) {
      System.err.println("Illegal Operand(s) for F1 format: " + mnemo[op]);
      bug(1);
    }
    return (op << 26 | a << 21 | b << 16 | half);
  }

  private static int F2(int op, int a, int b, int c) {
    if ((a & ~0x1F | b & ~0x1F | c & ~0x1F) != 0) {
      System.err.println("Illegal Operand(s) for F2 format: " + mnemo[op]);
      bug(1);
    }
    return (op << 26 | a << 21 | b << 16 | c);
  }

  private static int F3(int op, int c) {
    if (c < 0 || c > MEM_SIZE) {
      System.err.println(
        "Operand for F3 format is referencing non-existent memory location."
      );
      bug(1);
    }
    return (op << 26 | c);
  }

  public static int assemble(int op) {
    if (op != WRL) {
      System.err.println(
        "DLX.assemble: the only instruction without arguments is WRL!"
      );
      bug(1);
    }
    return F1(op, 0, 0, 0);
  }

  public static int assemble(int op, int arg1) {
    switch (op) {
      // F1 format
      case BSR:
        return F1(op, 0, 0, arg1);
      // F2 format
      case RET:
        return F2(op, 0, 0, arg1);
      case RDI:
      case RDF:
      case RDB:
        return F2(op, arg1, 0, 0);
      case WRI:
      case WRF:
      case WRB:
        return F2(op, 0, arg1, 0);
      // F3 format
      case JSR:
        return F3(op, arg1);
      // error
      case ERR:
      default:
        System.err.println(
          "DLX.assemble: wrong opcode for one arg instruction!"
        );
        bug(1);
        break;
    }
    return Integer.MIN_VALUE;
  }

  public static int assemble(int op, int arg1, int arg2) {
    switch (op) {
      // F1 format
      case CHKI:
      case BEQ:
      case BNE:
      case BLT:
      case BGE:
      case BLE:
      case BGT:
        return F1(op, arg1, 0, arg2);
      // F2 format
      case CHK:
        return F2(op, arg1, 0, arg2);
      // error
      case ERR:
      default:
        System.err.println(
          "DLX.assemble: wrong opcode for two arg instruction!"
        );
        bug(1);
        break;
    }
    return Integer.MIN_VALUE;
  }

  public static int assemble(int op, int arg1, int arg2, int arg3) {
    switch (op) {
      // F1 format
      case ADDI:
      case SUBI:
      case MULI:
      case DIVI:
      case MODI:
      case POWI:
      case CMPI:
      case ORI:
      case ANDI:
      case BICI:
      case XORI:
      case LSHI:
      case ASHI:
      case LDW:
      case POP:
      case STW:
      case PSH:
        return F1(op, arg1, arg2, arg3);
      // F2 format
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case MOD:
      case POW:
      case CMP:
      case fADD:
      case fSUB:
      case fMUL:
      case fDIV:
      case fMOD:
      case fCMP:
      case OR:
      case AND:
      case BIC:
      case XOR:
      case LSH:
      case ASH:
      case LDX:
      case STX:
      case ARRCPY:
        return F2(op, arg1, arg2, arg3);
      // error
      case ERR:
      default:
        System.err.println(
          "DLX.assemble: wrong opcode for three arg instruction!"
        );
        bug(1);
        break;
    }
    return Integer.MIN_VALUE;
  }

  public static int assemble(int op, int arg1, int arg2, float arg3) {
    switch (op) {
      // F1 format
      case fADDI:
      case fSUBI:
      case fMULI:
      case fDIVI:
      case fMODI:
      case fCMPI:
        return F1(op, arg1, arg2, arg3);
      // error
      case ERR:
      default:
        System.err.println(
          "DLX.assemble: wrong opcode for three arg instruction!"
        );
        bug(1);
        break;
    }
    return Integer.MIN_VALUE;
  }
}
